package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.ConversationResponse;
import dev.rupom.project.airbnb.dto.MessageResponse;
import dev.rupom.project.airbnb.dto.SendMessageRequest;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Conversation;
import dev.rupom.project.airbnb.entity.Message;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.exception.ForbiddenException;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.MessageMapper;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.ConversationRepository;
import dev.rupom.project.airbnb.repository.MessageRepository;
import dev.rupom.project.airbnb.repository.UserRepository;
import jakarta.transaction.Transactional;
import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.NotificationType;
import dev.rupom.project.airbnb.service.kafka.KafkaEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final KafkaEventProducer kafkaEventProducer;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Transactional
    @Override
    public MessageResponse sendMessage(Long senderUserId, SendMessageRequest request) {
        log.info("User {} sending message for conversation: {} or booking: {}",
                senderUserId, request.getConversationId(), request.getBookingId());

        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Conversation conversation = resolveConversation(senderUserId, request);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent().trim())
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessageContent(request.getContent().trim());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Determine recipient
        User recipient = conversation.getGuest().getId().equals(senderUserId)
                ? conversation.getHost()
                : conversation.getGuest();

        if (recipient != null && recipient.getEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(recipient.getEmail())
                    .recipientName(recipient.getName())
                    .subject("New message from " + sender.getName())
                    .message(sender.getName() + " sent you a message: \"" + request.getContent().trim() + "\"")
                    .notificationType(NotificationType.DIRECT_MESSAGE)
                    .timestamp(Instant.now())
                    .build());
        }

        log.info("Message sent successfully with id: {} in conversation: {}", savedMessage.getId(), conversation.getId());
        MessageResponse response = messageMapper.toMessageResponse(savedMessage);

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), response);
                log.info("Dispatched real-time WebSocket message to /topic/conversations/{}", conversation.getId());
            } catch (Exception e) {
                log.warn("WebSocket dispatch failed for conversation {}: {}", conversation.getId(), e.getMessage());
            }
        }

        return response;
    }

    @Transactional
    @Override
    public List<MessageResponse> getConversationMessages(Long userId, Long conversationId) {
        log.info("User {} fetching messages for conversation id: {}", userId, conversationId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        validateParticipant(conversation, userId);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Message> unreadToMark = new ArrayList<>();

        for (Message m : messages) {
            if (!m.getSender().getId().equals(userId) && Boolean.FALSE.equals(m.getIsRead())) {
                m.setIsRead(true);
                unreadToMark.add(m);
            }
        }

        if (!unreadToMark.isEmpty()) {
            messageRepository.saveAll(unreadToMark);
            log.info("Marked {} messages as read in conversation: {}", unreadToMark.size(), conversationId);
        }

        return messages.stream()
                .map(messageMapper::toMessageResponse)
                .toList();
    }

    @Override
    public List<ConversationResponse> getUserConversations(Long userId) {
        log.info("Fetching conversations for user id: {}", userId);
        List<Conversation> conversations = conversationRepository.findUserConversations(userId);

        return conversations.stream().map(c -> {
            ConversationResponse response = messageMapper.toConversationResponse(c);
            Long unreadCount = messageRepository.countUnreadInConversation(c.getId(), userId);
            response.setUnreadCount(unreadCount != null ? unreadCount : 0L);
            return response;
        }).toList();
    }

    @Override
    public Long getUnreadMessageCount(Long userId) {
        log.info("Counting total unread messages for user id: {}", userId);
        Long count = messageRepository.countUnreadMessagesForUser(userId);
        return count != null ? count : 0L;
    }

    private Conversation resolveConversation(Long userId, SendMessageRequest request) {
        if (request.getConversationId() != null) {
            Conversation conv = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + request.getConversationId()));
            validateParticipant(conv, userId);
            return conv;
        }

        if (request.getBookingId() != null) {
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

            User host = booking.getHotel().getOwner();
            User guest = booking.getUser();

            if (host == null) {
                throw new BadRequestException("Listing does not have an assigned host.");
            }

            if (!userId.equals(guest.getId()) && !userId.equals(host.getId())) {
                throw new ForbiddenException("You are neither the guest nor the host for this booking.");
            }

            return conversationRepository.findByBookingId(booking.getId())
                    .orElseGet(() -> {
                        log.info("Initializing new conversation for booking id: {}", booking.getId());
                        Conversation newConv = Conversation.builder()
                                .booking(booking)
                                .guest(guest)
                                .host(host)
                                .hotel(booking.getHotel())
                                .build();
                        return conversationRepository.save(newConv);
                    });
        }

        throw new BadRequestException("Either conversationId or bookingId must be provided.");
    }

    private void validateParticipant(Conversation conversation, Long userId) {
        boolean isGuest = conversation.getGuest().getId().equals(userId);
        boolean isHost = conversation.getHost().getId().equals(userId);
        if (!isGuest && !isHost) {
            throw new ForbiddenException("You are not a participant in this conversation.");
        }
    }
}

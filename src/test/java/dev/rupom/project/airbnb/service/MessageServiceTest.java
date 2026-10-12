package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.MessageResponse;
import dev.rupom.project.airbnb.dto.SendMessageRequest;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Conversation;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Message;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.exception.ForbiddenException;
import dev.rupom.project.airbnb.mapper.MessageMapper;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.ConversationRepository;
import dev.rupom.project.airbnb.repository.MessageRepository;
import dev.rupom.project.airbnb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private dev.rupom.project.airbnb.service.kafka.KafkaEventProducer kafkaEventProducer;

    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User guest;
    private User host;
    private Hotel hotel;
    private Booking booking;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        guest = User.builder().id(1L).name("Guest User").build();
        host = User.builder().id(2L).name("Host User").build();
        hotel = Hotel.builder().id(10L).name("City Loft").owner(host).build();

        booking = Booking.builder()
                .id(100L)
                .hotel(hotel)
                .user(guest)
                .build();

        conversation = Conversation.builder()
                .id(50L)
                .booking(booking)
                .guest(guest)
                .host(host)
                .hotel(hotel)
                .build();
    }

    @Test
    void sendMessage_viaBookingId_createsConversationAndMessage() {
        SendMessageRequest request = SendMessageRequest.builder()
                .bookingId(100L)
                .content("Hello, what time is check-in?")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(conversationRepository.findByBookingId(100L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> {
            Message m = i.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(MessageResponse.builder()
                .id(1L)
                .conversationId(50L)
                .senderId(1L)
                .content("Hello, what time is check-in?")
                .isRead(false)
                .build());

        MessageResponse response = messageService.sendMessage(1L, request);

        assertNotNull(response);
        assertEquals("Hello, what time is check-in?", response.getContent());
        assertFalse(response.getIsRead());
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(conversationRepository, atLeastOnce()).save(any(Conversation.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/conversations/" + conversation.getId()), any(MessageResponse.class));
    }

    @Test
    void sendMessage_notParticipant_throwsForbiddenException() {
        SendMessageRequest request = SendMessageRequest.builder()
                .bookingId(100L)
                .content("Spam message")
                .build();

        User thirdParty = User.builder().id(99L).name("Intruder").build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(thirdParty));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () -> messageService.sendMessage(99L, request));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getConversationMessages_marksIncomingAsRead() {
        Message unreadFromHost = Message.builder()
                .id(1L)
                .conversation(conversation)
                .sender(host)
                .content("Check-in is at 3 PM!")
                .isRead(false)
                .build();

        when(conversationRepository.findById(50L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of(unreadFromHost));
        when(messageMapper.toMessageResponse(unreadFromHost)).thenReturn(MessageResponse.builder()
                .id(1L)
                .content("Check-in is at 3 PM!")
                .isRead(true)
                .build());

        // Guest views the conversation
        List<MessageResponse> messages = messageService.getConversationMessages(1L, 50L);

        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertTrue(unreadFromHost.getIsRead());
        verify(messageRepository, times(1)).saveAll(any());
    }

    @Test
    void getUnreadMessageCount_returnsCount() {
        when(messageRepository.countUnreadMessagesForUser(1L)).thenReturn(4L);

        Long unreadCount = messageService.getUnreadMessageCount(1L);

        assertEquals(4L, unreadCount);
    }
}

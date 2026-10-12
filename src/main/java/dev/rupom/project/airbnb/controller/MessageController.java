package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.ConversationResponse;
import dev.rupom.project.airbnb.dto.MessageResponse;
import dev.rupom.project.airbnb.dto.SendMessageRequest;
import dev.rupom.project.airbnb.service.MessageService;
import dev.rupom.project.airbnb.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/conversations/messages")
    public ResponseEntity<MessageResponse> sendMessage(@RequestBody @Valid SendMessageRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} sending message", currentUserId);
        MessageResponse response = messageService.sendMessage(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bookings/{bookingId}/messages")
    public ResponseEntity<MessageResponse> sendBookingMessage(
            @PathVariable Long bookingId,
            @RequestBody @Valid SendMessageRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        request.setBookingId(bookingId);
        log.info("User {} sending message for booking id: {}", currentUserId, bookingId);
        MessageResponse response = messageService.sendMessage(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getMyConversations() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} fetching their conversation inbox", currentUserId);
        return ResponseEntity.ok(messageService.getUserConversations(currentUserId));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getConversationMessages(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} fetching messages for conversation id: {}", currentUserId, id);
        return ResponseEntity.ok(messageService.getConversationMessages(currentUserId, id));
    }

    @GetMapping("/conversations/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long count = messageService.getUnreadMessageCount(currentUserId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}

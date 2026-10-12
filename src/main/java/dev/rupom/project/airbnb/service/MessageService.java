package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.ConversationResponse;
import dev.rupom.project.airbnb.dto.MessageResponse;
import dev.rupom.project.airbnb.dto.SendMessageRequest;

import java.util.List;

public interface MessageService {

    MessageResponse sendMessage(Long senderUserId, SendMessageRequest request);

    List<MessageResponse> getConversationMessages(Long userId, Long conversationId);

    List<ConversationResponse> getUserConversations(Long userId);

    Long getUnreadMessageCount(Long userId);
}

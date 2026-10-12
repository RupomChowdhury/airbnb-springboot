package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE (m.conversation.guest.id = :userId OR m.conversation.host.id = :userId) AND m.sender.id != :userId AND m.isRead = false")
    Long countUnreadMessagesForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.isRead = false")
    Long countUnreadInConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}

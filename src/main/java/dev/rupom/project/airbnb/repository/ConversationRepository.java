package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByBookingId(Long bookingId);

    @Query("SELECT c FROM Conversation c WHERE c.guest.id = :userId OR c.host.id = :userId ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findUserConversations(@Param("userId") Long userId);
}

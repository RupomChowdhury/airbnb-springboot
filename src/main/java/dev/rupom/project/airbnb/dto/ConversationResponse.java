package dev.rupom.project.airbnb.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private Long id;
    private Long bookingId;
    private Long hotelId;
    private String hotelName;
    private Long guestId;
    private String guestName;
    private Long hostId;
    private String hostName;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
    private LocalDateTime createdAt;
}

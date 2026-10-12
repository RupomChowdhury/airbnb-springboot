package dev.rupom.project.airbnb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    private Long conversationId;

    private Long bookingId;

    @NotBlank(message = "Message content cannot be blank")
    private String content;
}

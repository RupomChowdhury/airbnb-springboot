package dev.rupom.project.airbnb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;
}

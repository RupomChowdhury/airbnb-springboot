package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestRequest {
    @NotBlank(message = "Guest name is required")
    private String name;

    @NotNull(message = "Guest gender is required")
    private Gender gender;

    private Integer age;
}

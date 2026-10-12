package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.Gender;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestResponse {
    private Long id;
    private String name;
    private Gender gender;
    private Integer age;
}

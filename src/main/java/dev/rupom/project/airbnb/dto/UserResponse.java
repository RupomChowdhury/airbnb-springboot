package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Set<UserRole> roles;
    private LocalDateTime createdAt;
}

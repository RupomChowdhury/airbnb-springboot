package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.ContactInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HotelResponse {
    private Long id;
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private ContactInfo contactInfo;
    private LocalDateTime createdAt;
    private Boolean active;
    private LocalDateTime updatedAt;
}

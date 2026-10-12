package dev.rupom.project.airbnb.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponse {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String hotelCity;
    private String[] photos;
    private LocalDateTime createdAt;
}

package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.ContactInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HotelRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String city;
    private String[] photos;
    private String[] amenities;
    private ContactInfo contactInfo;
    private Boolean active;
}

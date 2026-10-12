package dev.rupom.project.airbnb.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class ContactInfo {
    private String completeAddress;
    private String location;
    private String email;
    private String phoneNumber;
}

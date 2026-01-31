package dev.rupom.project.airbnb.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ContactInfo {
    private String completeAddress;
    private String location;
    private String email;
    private String phoneNumber;
}

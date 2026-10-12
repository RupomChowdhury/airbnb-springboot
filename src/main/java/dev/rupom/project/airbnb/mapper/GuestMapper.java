package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.GuestRequest;
import dev.rupom.project.airbnb.dto.GuestResponse;
import dev.rupom.project.airbnb.entity.Guest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GuestMapper {
    Guest toGuest(GuestRequest request);
    GuestResponse toGuestResponse(Guest guest);
}

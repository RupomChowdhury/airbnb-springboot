package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.BookingResponse;
import dev.rupom.project.airbnb.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {GuestMapper.class, PaymentMapper.class})
public interface BookingMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomType", source = "room.type")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    BookingResponse toBookingResponse(Booking booking);
}

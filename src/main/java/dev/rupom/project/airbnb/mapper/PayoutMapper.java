package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.PayoutResponse;
import dev.rupom.project.airbnb.entity.Payout;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayoutMapper {

    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "host.id", target = "hostId")
    @Mapping(source = "host.name", target = "hostName")
    PayoutResponse toPayoutResponse(Payout payout);
}

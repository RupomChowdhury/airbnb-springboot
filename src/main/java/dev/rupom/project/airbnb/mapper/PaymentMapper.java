package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.PaymentResponse;
import dev.rupom.project.airbnb.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "bookingId", source = "booking.id")
    PaymentResponse toPaymentResponse(Payment payment);
}

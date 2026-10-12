package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.ReviewResponse;
import dev.rupom.project.airbnb.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "bookingId", source = "booking.id")
    ReviewResponse toReviewResponse(Review review);
}

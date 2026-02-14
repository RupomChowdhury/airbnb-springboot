package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HotelMapper {
    Hotel toHotel(HotelRequest request);
    HotelResponse toHotelResponse(Hotel response);
}

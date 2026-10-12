package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.WishlistResponse;
import dev.rupom.project.airbnb.entity.Wishlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WishlistMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "hotelCity", source = "hotel.city")
    @Mapping(target = "photos", source = "hotel.photos")
    WishlistResponse toWishlistResponse(Wishlist wishlist);
}

package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.InventoryResponse;
import dev.rupom.project.airbnb.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "roomId", source = "room.id")
    InventoryResponse toInventoryResponse(Inventory inventory);
}

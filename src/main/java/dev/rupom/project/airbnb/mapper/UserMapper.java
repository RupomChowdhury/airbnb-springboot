package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.SignUpRequest;
import dev.rupom.project.airbnb.dto.UserResponse;
import dev.rupom.project.airbnb.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(SignUpRequest request);

    UserResponse toUserResponse(User user);
}

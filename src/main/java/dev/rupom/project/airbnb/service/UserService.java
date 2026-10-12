package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.UserProfileUpdateRequest;
import dev.rupom.project.airbnb.dto.UserResponse;

public interface UserService {
    UserResponse getUserProfile(Long userId);
    UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest request);
}

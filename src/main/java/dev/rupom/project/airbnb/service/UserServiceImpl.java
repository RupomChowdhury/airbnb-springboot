package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.config.RedisCacheConfig;
import dev.rupom.project.airbnb.dto.UserProfileUpdateRequest;
import dev.rupom.project.airbnb.dto.UserResponse;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.UserMapper;
import dev.rupom.project.airbnb.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final String USER_NOT_FOUND = "User not found.";

    @Cacheable(value = RedisCacheConfig.USER_PROFILE_CACHE, key = "#userId")
    @Override
    public UserResponse getUserProfile(Long userId) {
        log.info("Fetching profile for user id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @CacheEvict(value = RedisCacheConfig.USER_PROFILE_CACHE, key = "#userId")
    @Transactional
    @Override
    public UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        log.info("Updating profile for user id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        user.setName(request.getName());
        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user id: {}", userId);
        return userMapper.toUserResponse(updatedUser);
    }
}

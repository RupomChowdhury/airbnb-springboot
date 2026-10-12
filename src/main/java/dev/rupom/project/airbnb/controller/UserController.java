package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.UserProfileUpdateRequest;
import dev.rupom.project.airbnb.dto.UserResponse;
import dev.rupom.project.airbnb.service.UserService;
import dev.rupom.project.airbnb.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Fetching profile for authenticated user: {}", currentUserId);
        return ResponseEntity.ok(userService.getUserProfile(currentUserId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody @Valid UserProfileUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating profile for authenticated user: {}", currentUserId);
        return ResponseEntity.ok(userService.updateUserProfile(currentUserId, request));
    }
}

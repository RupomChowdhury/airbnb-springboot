package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.AuthResponse;
import dev.rupom.project.airbnb.dto.LoginRequest;
import dev.rupom.project.airbnb.dto.SignUpRequest;

public interface AuthService {
    AuthResponse signUp(SignUpRequest request);
    AuthResponse login(LoginRequest request);
}

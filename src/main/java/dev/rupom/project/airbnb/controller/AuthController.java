package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.AuthResponse;
import dev.rupom.project.airbnb.dto.LoginRequest;
import dev.rupom.project.airbnb.dto.SignUpRequest;
import dev.rupom.project.airbnb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final dev.rupom.project.airbnb.service.RateLimiterService rateLimiterService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        log.info("Received signup request for email: {}", request.getEmail());
        AuthResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = dev.rupom.project.airbnb.util.ClientIpUtils.getClientIp(httpRequest);
        rateLimiterService.checkRateLimit(
                "ratelimit:auth:login:" + clientIp,
                10,
                60,
                "Too many login attempts. Please wait 60 seconds before trying again."
        );
        log.info("Received login request for email: {} from IP: {}", request.getEmail(), clientIp);
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

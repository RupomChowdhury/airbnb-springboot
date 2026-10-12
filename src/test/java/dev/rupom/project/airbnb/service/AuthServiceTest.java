package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.AuthResponse;
import dev.rupom.project.airbnb.dto.LoginRequest;
import dev.rupom.project.airbnb.dto.SignUpRequest;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.entity.enums.UserRole;
import dev.rupom.project.airbnb.exception.ConflictException;
import dev.rupom.project.airbnb.repository.UserRepository;
import dev.rupom.project.airbnb.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Rupom Chowdhury")
                .email("rupom@example.com")
                .password("encoded_pass")
                .roles(Set.of(UserRole.User))
                .build();
    }

    @Test
    void signUp_success() {
        SignUpRequest request = SignUpRequest.builder()
                .name("Rupom Chowdhury")
                .email("rupom@example.com")
                .password("password123")
                .roles(Set.of(UserRole.User))
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock_token");

        AuthResponse response = authService.signUp(request);

        assertNotNull(response);
        assertEquals("mock_token", response.getToken());
        assertEquals("rupom@example.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void signUp_emailAlreadyExists_throwsConflictException() {
        SignUpRequest request = SignUpRequest.builder()
                .name("Rupom Chowdhury")
                .email("rupom@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("rupom@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.signUp(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success() {
        LoginRequest request = LoginRequest.builder()
                .email("rupom@example.com")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("rupom@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mock_token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_token", response.getToken());
        assertEquals("rupom@example.com", response.getEmail());
    }
}

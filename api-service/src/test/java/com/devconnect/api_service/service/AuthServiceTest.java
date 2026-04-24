package com.devconnect.api_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devconnect.api_service.dto.AuthResponse;
import com.devconnect.api_service.dto.LoginRequest;
import com.devconnect.api_service.dto.RegisterRequest;
import com.devconnect.api_service.exception.ConflictException;
import com.devconnect.api_service.exception.UnauthorizedException;
import com.devconnect.api_service.model.User;
import com.devconnect.api_service.repository.UserRepository;
import com.devconnect.api_service.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterWhenDataIsValid() {
        RegisterRequest request = new RegisterRequest(
                "Paulo Dev",
                "paulo@example.com",
                "password123",
                "paulodev",
                "Java developer"
        );

        User savedUser = new User();
        savedUser.setEmail("paulo@example.com");

        when(userRepository.existsByEmail("paulo@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("paulodev")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateAccessToken("paulo@example.com")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("paulo@example.com")).thenReturn("refresh-token");
        when(jwtUtil.getAccessExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.register(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900000L, response.expiresIn());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "Paulo Dev",
                "paulo@example.com",
                "password123",
                "paulodev",
                "Java developer"
        );

        when(userRepository.existsByEmail("paulo@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
    }

    @Test
    void shouldLoginWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("paulo@example.com", "password123");
        User user = new User();
        user.setEmail("paulo@example.com");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("paulo@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken("paulo@example.com")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("paulo@example.com")).thenReturn("refresh-token");
        when(jwtUtil.getAccessExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900000L, response.expiresIn());
    }

    @Test
    void shouldThrowUnauthorizedWhenPasswordIsInvalid() {
        LoginRequest request = new LoginRequest("paulo@example.com", "wrong-password");
        User user = new User();
        user.setEmail("paulo@example.com");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("paulo@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }
}

package com.devconnect.api_service.service;

import com.devconnect.api_service.dto.AuthResponse;
import com.devconnect.api_service.dto.LoginRequest;
import com.devconnect.api_service.dto.RefreshTokenRequest;
import com.devconnect.api_service.dto.RegisterRequest;
import com.devconnect.api_service.exception.ConflictException;
import com.devconnect.api_service.exception.UnauthorizedException;
import com.devconnect.api_service.model.User;
import com.devconnect.api_service.repository.UserRepository;
import com.devconnect.api_service.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already in use");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUsername(request.username());
        user.setBio(request.bio());

        User savedUser = userRepository.save(user);
        return generateAuthResponse(savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return generateAuthResponse(user.getEmail());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtUtil.isRefreshTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String email = jwtUtil.extractSubject(refreshToken);
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        return generateAuthResponse(email);
    }

    private AuthResponse generateAuthResponse(String subject) {
        String accessToken = jwtUtil.generateAccessToken(subject);
        String refreshToken = jwtUtil.generateRefreshToken(subject);

        return new AuthResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtUtil.getAccessExpirationMs()
        );
    }
}

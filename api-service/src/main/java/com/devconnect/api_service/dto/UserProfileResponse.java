package com.devconnect.api_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String username,
        String bio,
        LocalDateTime createdAt
) {
}

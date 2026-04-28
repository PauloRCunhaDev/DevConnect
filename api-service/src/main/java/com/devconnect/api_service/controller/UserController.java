package com.devconnect.api_service.controller;

import com.devconnect.api_service.dto.UpdateUserProfileRequest;
import com.devconnect.api_service.dto.UserProfileResponse;
import com.devconnect.api_service.exception.UnauthorizedException;
import com.devconnect.api_service.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getPublicProfile(@PathVariable String username) {
        UserProfileResponse response = userService.getPublicProfileByUsername(username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Unauthorized");
        }

        UserProfileResponse response = userService.updateCurrentUserProfile(principal.getName(), request);
        return ResponseEntity.ok(response);
    }
}

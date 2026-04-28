package com.devconnect.api_service.service;

import com.devconnect.api_service.dto.UpdateUserProfileRequest;
import com.devconnect.api_service.dto.UserProfileResponse;
import com.devconnect.api_service.exception.ResourceNotFoundException;
import com.devconnect.api_service.model.User;
import com.devconnect.api_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getPublicProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getBio(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(String email, UpdateUserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(request.name());
        user.setBio(request.bio());

        User savedUser = userRepository.save(user);

        return new UserProfileResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getUsername(),
                savedUser.getBio(),
                savedUser.getCreatedAt()
        );
    }
}

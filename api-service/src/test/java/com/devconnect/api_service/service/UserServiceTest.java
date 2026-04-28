package com.devconnect.api_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.devconnect.api_service.dto.UserProfileResponse;
import com.devconnect.api_service.exception.ResourceNotFoundException;
import com.devconnect.api_service.model.User;
import com.devconnect.api_service.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUserProfileWhenUsernameExists() {
        User user = new User();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setName("Paulo Dev");
        user.setUsername("paulodev");
        user.setBio("Java developer");
        user.setCreatedAt(LocalDateTime.of(2026, 4, 28, 10, 0));

        when(userRepository.findByUsername("paulodev")).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getPublicProfileByUsername("paulodev");

        assertEquals("Paulo Dev", response.name());
        assertEquals("paulodev", response.username());
        assertEquals("Java developer", response.bio());
    }

    @Test
    void shouldThrowNotFoundWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getPublicProfileByUsername("unknown"));
    }
}

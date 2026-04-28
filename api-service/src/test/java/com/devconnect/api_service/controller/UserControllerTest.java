package com.devconnect.api_service.controller;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devconnect.api_service.config.JwtAuthenticationFilter;
import com.devconnect.api_service.dto.UpdateUserProfileRequest;
import com.devconnect.api_service.dto.UserProfileResponse;
import com.devconnect.api_service.exception.GlobalExceptionHandler;
import com.devconnect.api_service.exception.ResourceNotFoundException;
import com.devconnect.api_service.service.UserService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnUserProfileWhenUsernameExists() throws Exception {
        UserProfileResponse profileResponse = new UserProfileResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Paulo Dev",
                "paulodev",
                "Java developer",
                LocalDateTime.of(2026, 4, 28, 10, 0)
        );

        when(userService.getPublicProfileByUsername("paulodev")).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/paulodev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paulo Dev"))
                .andExpect(jsonPath("$.username").value("paulodev"))
                .andExpect(jsonPath("$.bio").value("Java developer"));
    }

    @Test
    void shouldReturnNotFoundWhenUsernameDoesNotExist() throws Exception {
        when(userService.getPublicProfileByUsername("unknown"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldUpdateCurrentUserProfile() throws Exception {
        UserProfileResponse profileResponse = new UserProfileResponse(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "New Name",
                "paulodev",
                "New bio",
                LocalDateTime.of(2026, 4, 28, 10, 0)
        );

        when(userService.updateCurrentUserProfile(anyString(), any(UpdateUserProfileRequest.class)))
                .thenReturn(profileResponse);

        String body = """
                {
                  "name": "New Name",
                  "bio": "New bio"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .principal(() -> "paulo@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.bio").value("New bio"));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatePayloadIsInvalid() throws Exception {
        String body = """
                {
                  "name": "",
                  "bio": "Bio"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .principal(() -> "paulo@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

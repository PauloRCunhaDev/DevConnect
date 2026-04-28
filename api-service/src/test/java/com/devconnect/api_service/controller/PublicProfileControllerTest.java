package com.devconnect.api_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devconnect.api_service.config.JwtAuthenticationFilter;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnPublicProfileWhenUsernameExists() throws Exception {
        UserProfileResponse profileResponse = new UserProfileResponse(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "Paulo Dev",
                "paulodev",
                "Java developer",
                LocalDateTime.of(2026, 4, 28, 10, 0)
        );

        when(userService.getPublicProfileByUsername("paulodev")).thenReturn(profileResponse);

        mockMvc.perform(get("/u/paulodev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paulo Dev"))
                .andExpect(jsonPath("$.username").value("paulodev"))
                .andExpect(jsonPath("$.bio").value("Java developer"));
    }

    @Test
    void shouldReturnNotFoundWhenUsernameDoesNotExist() throws Exception {
        when(userService.getPublicProfileByUsername("unknown"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/u/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}

package com.devconnect.api_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devconnect.api_service.dto.AuthResponse;
import com.devconnect.api_service.config.JwtAuthenticationFilter;
import com.devconnect.api_service.exception.ConflictException;
import com.devconnect.api_service.exception.GlobalExceptionHandler;
import com.devconnect.api_service.exception.UnauthorizedException;
import com.devconnect.api_service.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnCreatedWhenRegisterPayloadIsValid() throws Exception {
        AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 900000L);
        when(authService.register(any())).thenReturn(response);

        String body = """
                {
                  "name": "Paulo Dev",
                  "email": "paulo@example.com",
                  "password": "password123",
                  "username": "paulodev",
                  "bio": "Java developer"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900000));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterPayloadIsInvalid() throws Exception {
        String body = """
                {
                  "name": "",
                  "email": "invalid",
                  "password": "123",
                  "username": "",
                  "bio": "bio"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnConflictWhenRegisterDataAlreadyExists() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("Email already in use"));

        String body = """
                {
                  "name": "Paulo Dev",
                  "email": "paulo@example.com",
                  "password": "password123",
                  "username": "paulodev",
                  "bio": "Java developer"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnOkWhenLoginPayloadIsValid() throws Exception {
        AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 900000L);
        when(authService.login(any())).thenReturn(response);

        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<String, String>() {{
            put("email", "paulo@example.com");
            put("password", "password123");
        }});

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900000));
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginCredentialsAreInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid credentials"));

        String body = """
                {
                  "email": "paulo@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshTokenIsInvalid() throws Exception {
        when(authService.refresh(any())).thenThrow(new UnauthorizedException("Invalid refresh token"));

        String body = """
                {
                  "refreshToken": "invalid-token"
                }
                """;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"))
                .andExpect(jsonPath("$.status").value(401));
    }
}

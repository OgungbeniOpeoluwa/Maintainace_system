package com.miva.maintenance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miva.maintenance.dto.AuthResponse;
import com.miva.maintenance.dto.LoginRequest;
import com.miva.maintenance.dto.RegisterRequest;
import com.miva.maintenance.model.Role;
import com.miva.maintenance.security.JwtAuthFilter;
import com.miva.maintenance.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security filters are disabled here (addFilters = false) because this test targets the
 * controller/HTTP contract in isolation — RBAC on protected endpoints is covered separately
 * by the service-layer tests and manual Swagger verification described in the README.
 *
 * JwtAuthFilter is mocked (not just filtered out of execution) because @WebMvcTest
 * auto-registers any @Component implementing Filter — JwtAuthFilter qualifies — so Spring
 * still needs a bean of that type to build the test context, even though addFilters=false
 * stops it from actually running during the request. Mocking it means Spring never has to
 * construct the real one, which would otherwise fail: its constructor needs JwtService and
 * CustomUserDetailsService, both @Service beans that this narrow slice deliberately excludes.
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;
    @MockBean private JwtAuthFilter jwtAuthFilter;

    @Test
    void registerReturns200WithATokenForAValidPayload() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@miva.university");
        request.setPassword("secret1");

        AuthResponse response = AuthResponse.builder()
                .token("fake-jwt-token")
                .userId("user-1")
                .fullName("Jane Doe")
                .email("jane@miva.university")
                .role(Role.STUDENT)
                .mustChangePassword(false)
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void registerReturns400WhenEmailIsMissing() throws Exception {
        String payloadMissingEmail = "{\"fullName\":\"Jane Doe\",\"password\":\"secret1\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadMissingEmail))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns400WhenPasswordIsTooShort() throws Exception {
        String payloadShortPassword = "{\"fullName\":\"Jane Doe\",\"email\":\"jane@miva.university\",\"password\":\"123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadShortPassword))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturns200WithATokenForValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@miva.university");
        request.setPassword("secret1");

        AuthResponse response = AuthResponse.builder()
                .token("fake-jwt-token-2")
                .userId("user-1")
                .fullName("Jane Doe")
                .email("jane@miva.university")
                .role(Role.STUDENT)
                .mustChangePassword(false)
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token-2"));
    }
}
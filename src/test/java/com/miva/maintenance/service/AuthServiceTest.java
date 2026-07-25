package com.miva.maintenance.service;

import com.miva.maintenance.dto.AuthResponse;
import com.miva.maintenance.dto.LoginRequest;
import com.miva.maintenance.dto.RegisterRequest;
import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.User;
import com.miva.maintenance.repository.UserRepository;
import com.miva.maintenance.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void registerCreatesAStudentStaffAccountWithAnEncodedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@miva.university");
        request.setPassword("plainPassword");
        request.setDepartment("Computer Science");

        when(userRepository.existsByEmail("jane@miva.university")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("user-1");
            return saved;
        });
        when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getRole()).isEqualTo(Role.STUDENT_STAFF);
        assertThat(response.getEmail()).isEqualTo("jane@miva.university");

        // Public registration must never let someone hand themselves OFFICER/ADMIN.
        verify(userRepository).save(argThatUserIsStudentStaffWithEncodedPassword());
    }

    @Test
    void registerRejectsAnEmailThatAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@miva.university");
        request.setPassword("plainPassword");

        when(userRepository.existsByEmail("jane@miva.university")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void loginReturnsATokenForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@miva.university");
        request.setPassword("plainPassword");

        User existingUser = User.builder()
                .id("user-1")
                .fullName("Jane Doe")
                .email("jane@miva.university")
                .password("ENCODED")
                .role(Role.STUDENT_STAFF)
                .active(true)
                .build();

        when(userRepository.findByEmail("jane@miva.university")).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getUserId()).isEqualTo("user-1");
    }

    private User argThatUserIsStudentStaffWithEncodedPassword() {
        return org.mockito.ArgumentMatchers.argThat(user ->
                user.getRole() == Role.STUDENT_STAFF
                        && "ENCODED".equals(user.getPassword())
                        && "jane@miva.university".equals(user.getEmail())
        );
    }
}

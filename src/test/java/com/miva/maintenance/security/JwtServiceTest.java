package com.miva.maintenance.security;

import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // @Value fields aren't populated without a Spring context, so set them directly for this unit test.
        ReflectionTestUtils.setField(jwtService, "secret", "unit-test-secret-key-must-be-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    private UserPrincipal principal(String id, String email) {
        User user = User.builder()
                .id(id)
                .fullName("Test User")
                .email(email)
                .password("hashed")
                .role(Role.STUDENT_STAFF)
                .active(true)
                .build();
        return new UserPrincipal(user);
    }

    @Test
    void generatesTokenAndExtractsTheCorrectUsername() {
        UserPrincipal user = principal("1", "jane@miva.university");

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("jane@miva.university");
    }

    @Test
    void tokenIsValidForTheUserItWasIssuedTo() {
        UserPrincipal user = principal("1", "jane@miva.university");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsNotValidForADifferentUser() {
        UserPrincipal owner = principal("1", "jane@miva.university");
        UserPrincipal someoneElse = principal("2", "other@miva.university");

        String token = jwtService.generateToken(owner);

        assertThat(jwtService.isTokenValid(token, someoneElse)).isFalse();
    }
}

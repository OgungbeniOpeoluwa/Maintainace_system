package com.miva.maintenance.controller;

import com.miva.maintenance.dto.CreateOfficerRequest;
import com.miva.maintenance.dto.CreateOfficerResponse;
import com.miva.maintenance.dto.UserResponse;
import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.User;
import com.miva.maintenance.repository.UserRepository;
import com.miva.maintenance.service.EmailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Administration")
public class AdminController {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> allUsers(@RequestParam(required = false) Role role) {
        List<User> users = role != null ? userRepository.findByRole(role) : userRepository.findAll();
        return ResponseEntity.ok(users.stream().map(UserResponse::from).toList());
    }

    @GetMapping("/officers")
    public ResponseEntity<List<UserResponse>> officers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.OFFICER)
                .map(UserResponse::from)
                .toList());
    }

    /**
     * Creates a Maintenance Officer account: generates a temporary password, assigns the officer
     * to one or more request categories, and emails them their login credentials.
     */
    @PostMapping("/officers")
    public ResponseEntity<CreateOfficerResponse> createOfficer(@Valid @RequestBody CreateOfficerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A user with this email already exists");
        }

        String tempPassword = generateTempPassword();

        User officer = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.OFFICER)
                .categoryIds(request.getCategoryIds())
                .mustChangePassword(true)
                .active(true)
                .build();

        officer = userRepository.save(officer);

        boolean emailSent = emailService.sendOfficerWelcomeEmail(
                officer.getEmail(), officer.getFullName(), tempPassword);

        return ResponseEntity.ok(CreateOfficerResponse.builder()
                .officer(UserResponse.from(officer))
                .emailSent(emailSent)
                // Always include the temp password in the response too — if email isn't configured
                // or fails to send, the admin still needs a way to hand it to the officer.
                .temporaryPassword(tempPassword)
                .build());
    }

    /** Promote/demote a user's role directly (kept for flexibility / Swagger use). */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(Role.valueOf(body.get("role")));
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(false);
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder("Mv");
        for (int i = 0; i < 8; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}

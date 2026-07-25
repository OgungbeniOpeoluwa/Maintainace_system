package com.miva.maintenance.service;

import com.miva.maintenance.dto.AuthResponse;
import com.miva.maintenance.dto.ChangePasswordRequest;
import com.miva.maintenance.dto.LoginRequest;
import com.miva.maintenance.dto.RegisterRequest;
import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.User;
import com.miva.maintenance.repository.UserRepository;
import com.miva.maintenance.security.JwtService;
import com.miva.maintenance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Public self-registration can only create STUDENT or STAFF accounts — never
        // OFFICER/ADMIN, regardless of what a caller puts in the request body.
        Role role = getRole(request);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .department(request.getDepartment())
                .role(role)
                .active(true)
                .build();

        user = userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    @NonNull
    private static Role getRole(RegisterRequest request) {
        Role requestedRole = request.getRole();
        Role role = (requestedRole == Role.STUDENT || requestedRole == Role.STAFF)
                ? requestedRole
                : Role.STUDENT;

        if (role == Role.STAFF && (request.getDepartment() == null || request.getDepartment().isBlank())) {
            throw new IllegalArgumentException("Department is required for staff accounts");
        }
        return role;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
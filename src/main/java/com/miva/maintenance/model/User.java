package com.miva.maintenance.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotBlank
    private String fullName;

    @Indexed(unique = true)
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password; // stored as BCrypt hash

    private Role role;

    private String department; // e.g. hostel block, faculty, office - optional

    // For OFFICER role only: which request categories this officer handles (e.g. Electrical, Plumbing)
    @Builder.Default
    private java.util.List<String> categoryIds = new java.util.ArrayList<>();

    // True right after an admin creates the account with a temp password; forces a password change on first login
    @Builder.Default
    private boolean mustChangePassword = false;

    private boolean active = true;

    @CreatedDate
    private Instant createdAt;
}

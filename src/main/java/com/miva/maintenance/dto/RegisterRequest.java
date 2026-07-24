package com.miva.maintenance.dto;

import com.miva.maintenance.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String fullName;

    @Email @NotBlank
    private String email;

    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String department;

    // Optional: only ADMIN accounts should normally be created by another admin.
    // Public registration defaults to STUDENT_STAFF unless specified as OFFICER by an admin call.
    private Role role;
}

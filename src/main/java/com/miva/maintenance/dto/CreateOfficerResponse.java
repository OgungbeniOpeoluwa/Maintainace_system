package com.miva.maintenance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOfficerResponse {
    private UserResponse officer;
    private boolean emailSent;
    // Fallback: if email sending isn't configured/fails, the admin still needs a way to
    // hand the officer their credentials, so we return the temp password here too.
    private String temporaryPassword;
}

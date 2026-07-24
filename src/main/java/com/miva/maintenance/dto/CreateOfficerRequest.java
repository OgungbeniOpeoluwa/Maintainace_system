package com.miva.maintenance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOfficerRequest {
    @NotBlank
    private String fullName;

    @Email @NotBlank
    private String email;

    @NotEmpty(message = "Select at least one category for this officer")
    private List<String> categoryIds;
}

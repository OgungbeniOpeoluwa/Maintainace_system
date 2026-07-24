package com.miva.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServiceRequestDto {
    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String categoryId;

    @NotBlank
    private String location;

    private String priority; // LOW, MEDIUM, HIGH
}

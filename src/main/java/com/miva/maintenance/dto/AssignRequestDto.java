package com.miva.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignRequestDto {
    @NotBlank
    private String officerId;
    private String notes;
}

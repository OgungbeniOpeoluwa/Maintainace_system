package com.miva.maintenance.dto;

import com.miva.maintenance.model.RequestStatus;
import lombok.Data;

@Data
public class StatusUpdateDto {
    private RequestStatus status;
    private String comment;
}

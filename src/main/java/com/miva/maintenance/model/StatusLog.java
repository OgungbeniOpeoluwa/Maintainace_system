package com.miva.maintenance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "status_logs")
public class StatusLog {

    @Id
    private String id;

    private String requestId;
    private RequestStatus status;
    private String updatedBy;   // user id
    private String comment;

    @CreatedDate
    private Instant timestamp;
}

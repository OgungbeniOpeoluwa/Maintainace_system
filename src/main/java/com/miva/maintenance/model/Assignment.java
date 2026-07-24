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
@Document(collection = "assignments")
public class Assignment {

    @Id
    private String id;

    private String requestId;
    private String officerId;
    private String assignedBy;   // admin user id
    private String notes;

    @CreatedDate
    private Instant assignedAt;
}

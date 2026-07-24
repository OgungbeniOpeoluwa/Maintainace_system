package com.miva.maintenance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "service_requests")
public class ServiceRequest {

    @Id
    private String id;

    private String title;
    private String description;
    private String categoryId;
    private String location;        // e.g. "Hostel Block C, Room 12"
    private String priority;        // LOW, MEDIUM, HIGH

    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    private String submittedBy;     // User id
    private String assignedTo;      // User id (officer), nullable
    private String imageUrl;        // uploaded evidence photo, nullable

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

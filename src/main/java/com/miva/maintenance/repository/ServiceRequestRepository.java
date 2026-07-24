package com.miva.maintenance.repository;

import com.miva.maintenance.model.RequestStatus;
import com.miva.maintenance.model.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServiceRequestRepository extends MongoRepository<ServiceRequest, String> {
    Page<ServiceRequest> findBySubmittedBy(String submittedBy, Pageable pageable);
    Page<ServiceRequest> findByAssignedTo(String assignedTo, Pageable pageable);
    Page<ServiceRequest> findByStatus(RequestStatus status, Pageable pageable);
    Page<ServiceRequest> findByCategoryId(String categoryId, Pageable pageable);
    Page<ServiceRequest> findByCategoryIdInAndAssignedToIsNullAndStatus(
            java.util.List<String> categoryIds, RequestStatus status, Pageable pageable);
}

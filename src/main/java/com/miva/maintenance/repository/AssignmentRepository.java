package com.miva.maintenance.repository;

import com.miva.maintenance.model.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {
    List<Assignment> findByRequestId(String requestId);
    List<Assignment> findByOfficerId(String officerId);
}

package com.miva.maintenance.repository;

import com.miva.maintenance.model.StatusLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StatusLogRepository extends MongoRepository<StatusLog, String> {
    List<StatusLog> findByRequestIdOrderByTimestampDesc(String requestId);
}

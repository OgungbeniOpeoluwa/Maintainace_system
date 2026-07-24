package com.miva.maintenance.repository;

import com.miva.maintenance.model.RequestCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RequestCategoryRepository extends MongoRepository<RequestCategory, String> {
}

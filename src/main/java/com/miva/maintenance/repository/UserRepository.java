package com.miva.maintenance.repository;

import com.miva.maintenance.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<User> findByRole(com.miva.maintenance.model.Role role);
    long countByRole(com.miva.maintenance.model.Role role);
}

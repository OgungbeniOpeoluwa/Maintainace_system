package com.miva.maintenance.controller;

import com.miva.maintenance.model.RequestCategory;
import com.miva.maintenance.repository.RequestCategoryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Request Categories")
public class RequestCategoryController {

    private final RequestCategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<RequestCategory>> all() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<RequestCategory> create(@RequestBody RequestCategory category) {
        return ResponseEntity.ok(categoryRepository.save(category));
    }
}

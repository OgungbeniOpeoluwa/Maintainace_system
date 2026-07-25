package com.miva.maintenance.controller;

import com.miva.maintenance.dto.AssignRequestDto;
import com.miva.maintenance.dto.ServiceRequestDto;
import com.miva.maintenance.dto.StatusUpdateDto;
import com.miva.maintenance.model.RequestStatus;
import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.ServiceRequest;
import com.miva.maintenance.security.UserPrincipal;
import com.miva.maintenance.service.FileStorageService;
import com.miva.maintenance.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Service Requests")
public class ServiceRequestController {

    private final ServiceRequestService requestService;
    private final FileStorageService fileStorageService;

    /** Submit a new maintenance/service request. Any authenticated user can submit. */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ServiceRequest> submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("request") @Valid ServiceRequestDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        String imageUrl = fileStorageService.store(image);
        ServiceRequest created = requestService.submit(
                dto, principal.getUser().getId(), imageUrl,
                principal.getUser().getFullName(), principal.getUser().getDepartment());
        return ResponseEntity.ok(created);
    }

    /** List requests — scoped by role: student/staff see their own, officer sees assigned, admin sees all (optionally filtered by status). */
    @GetMapping
    public ResponseEntity<Page<ServiceRequest>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Role role = principal.getUser().getRole();

        Page<ServiceRequest> result;
        if (role == Role.ADMIN) {
            result = status != null ? requestService.findByStatus(status, pageable) : requestService.findAll(pageable);
        } else if (role == Role.OFFICER) {
            result = requestService.findForOfficer(principal.getUser().getId(), pageable);
        } else {
            // STUDENT and STAFF both see their own submissions here.
            result = requestService.findForStudent(principal.getUser().getId(), pageable);
        }
        return ResponseEntity.ok(result);
    }

    /** Staff-only: every request submitted by anyone in the staff member's department. Read visibility only. */
    @GetMapping("/department")
    @PreAuthorize("hasAuthority('ROLE_STAFF')")
    public ResponseEntity<Page<ServiceRequest>> departmentRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(requestService.findForDepartment(principal.getUser().getDepartment(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequest> getOne(@PathVariable String id) {
        return ResponseEntity.ok(requestService.findById(id));
    }

    /** Officer-only: unassigned requests matching the officer's category specialization(s). */
    @GetMapping("/available")
    @PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<Page<ServiceRequest>> available(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(requestService.findAvailableForOfficer(
                principal.getUser().getCategoryIds(), pageable));
    }

    /** Officer self-claims an unassigned request in their category. */
    @PutMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<ServiceRequest> claim(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.claim(id, principal.getUser().getId()));
    }

    /** Admin assigns a request to a maintenance officer. */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServiceRequest> assign(
            @PathVariable String id,
            @Valid @RequestBody AssignRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.assign(id, dto, principal.getUser().getId()));
    }

    /** Officer (or admin) updates the status of a request. */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_OFFICER','ROLE_ADMIN')")
    public ResponseEntity<ServiceRequest> updateStatus(
            @PathVariable String id,
            @RequestBody StatusUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.updateStatus(id, dto, principal.getUser().getId()));
    }

    /** Delete a request. Admins can delete any; students/staff can only delete their own PENDING requests. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        requestService.delete(id, principal.getUser().getId(), principal.getUser().getRole());
        return ResponseEntity.noContent().build();
    }
}

package com.miva.maintenance.service;

import com.miva.maintenance.dto.AssignRequestDto;
import com.miva.maintenance.dto.ServiceRequestDto;
import com.miva.maintenance.dto.StatusUpdateDto;
import com.miva.maintenance.model.*;
import com.miva.maintenance.repository.AssignmentRepository;
import com.miva.maintenance.repository.ServiceRequestRepository;
import com.miva.maintenance.repository.StatusLogRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final AssignmentRepository assignmentRepository;
    private final StatusLogRepository statusLogRepository;
    private final MongoTemplate mongoTemplate;

    public ServiceRequest submit(ServiceRequestDto dto, String submitterId, String imageUrl,
                                 String submitterName, String submitterDepartment) {
        ServiceRequest req = ServiceRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .categoryId(dto.getCategoryId())
                .location(dto.getLocation())
                .priority(dto.getPriority() == null ? "MEDIUM" : dto.getPriority())
                .status(RequestStatus.PENDING)
                .submittedBy(submitterId)
                .submitterName(submitterName)
                .submitterDepartment(submitterDepartment)
                .imageUrl(imageUrl)
                .build();
        req = requestRepository.save(req);
        logStatus(req.getId(), RequestStatus.PENDING, submitterId, "Request submitted");
        return req;
    }

    public Page<ServiceRequest> findForStudent(String userId, Pageable pageable) {
        return requestRepository.findBySubmittedBy(userId, pageable);
    }

    /** Every request submitted by anyone in the given department — read visibility for Staff. */
    public Page<ServiceRequest> findForDepartment(String department, Pageable pageable) {
        if (department == null || department.isBlank()) {
            return Page.empty(pageable);
        }
        return requestRepository.findBySubmitterDepartment(department, pageable);
    }

    public Page<ServiceRequest> findForOfficer(String officerId, Pageable pageable) {
        return requestRepository.findByAssignedTo(officerId, pageable);
    }

    public Page<ServiceRequest> findAll(Pageable pageable) {
        return requestRepository.findAll(pageable);
    }

    public Page<ServiceRequest> findByStatus(RequestStatus status, Pageable pageable) {
        return requestRepository.findByStatus(status, pageable);
    }

    /** Unassigned, still-pending requests that match one of the officer's assigned categories. */
    public Page<ServiceRequest> findAvailableForOfficer(List<String> categoryIds, Pageable pageable) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return requestRepository.findByCategoryIdInAndAssignedToIsNullAndStatus(
                categoryIds, RequestStatus.PENDING, pageable);
    }

    /**
     * Officer self-claims an unassigned request. Uses an atomic find-and-modify so that if two
     * officers click "Claim" on the same request at the same time, only one of them succeeds.
     */
    public ServiceRequest claim(String requestId, String officerId) {
        Query query = new Query(Criteria.where("id").is(requestId)
                .and("assignedTo").isNull()
                .and("status").is(RequestStatus.PENDING));
        Update update = new Update()
                .set("assignedTo", officerId)
                .set("status", RequestStatus.ASSIGNED);

        UpdateResult result = mongoTemplate.updateFirst(query, update, ServiceRequest.class);
        if (result.getModifiedCount() == 0) {
            throw new IllegalArgumentException("This request is no longer available — it may already be claimed.");
        }

        assignmentRepository.save(Assignment.builder()
                .requestId(requestId)
                .officerId(officerId)
                .assignedBy(officerId) // self-claimed
                .notes("Self-claimed by officer")
                .build());

        logStatus(requestId, RequestStatus.ASSIGNED, officerId, "Claimed by officer");
        return findById(requestId);
    }

    public ServiceRequest findById(String id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
    }

    public ServiceRequest assign(String requestId, AssignRequestDto dto, String adminId) {
        ServiceRequest req = findById(requestId);
        req.setAssignedTo(dto.getOfficerId());
        req.setStatus(RequestStatus.ASSIGNED);
        req = requestRepository.save(req);

        assignmentRepository.save(Assignment.builder()
                .requestId(requestId)
                .officerId(dto.getOfficerId())
                .assignedBy(adminId)
                .notes(dto.getNotes())
                .build());

        logStatus(requestId, RequestStatus.ASSIGNED, adminId,
                "Assigned to officer " + dto.getOfficerId());
        return req;
    }

    public ServiceRequest updateStatus(String requestId, StatusUpdateDto dto, String updaterId) {
        ServiceRequest req = findById(requestId);
        req.setStatus(dto.getStatus());
        req = requestRepository.save(req);
        logStatus(requestId, dto.getStatus(), updaterId, dto.getComment());
        return req;
    }

    /**
     * Deletes a request. Admins can delete any request at any time. Students/Staff can only
     * delete a request they submitted themselves, and only while it's still PENDING — once an
     * officer has been assigned or work has started, it's out of the submitter's hands.
     */
    public void delete(String requestId, String userId, Role requesterRole) {
        ServiceRequest req = findById(requestId);

        if (requesterRole != Role.ADMIN) {
            boolean isOwner = req.getSubmittedBy() != null && req.getSubmittedBy().equals(userId);
            if (!isOwner) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only delete requests you submitted yourself.");
            }
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException(
                        "This request can no longer be deleted — it has already been assigned or actioned.");
            }
        }

        requestRepository.deleteById(requestId);
        assignmentRepository.deleteByRequestId(requestId);
        statusLogRepository.deleteByRequestId(requestId);
    }

    private void logStatus(String requestId, RequestStatus status, String updatedBy, String comment) {
        statusLogRepository.save(StatusLog.builder()
                .requestId(requestId)
                .status(status)
                .updatedBy(updatedBy)
                .comment(comment)
                .build());
    }
}

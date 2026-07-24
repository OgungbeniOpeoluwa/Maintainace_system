package com.miva.maintenance.controller;

import com.miva.maintenance.model.RequestCategory;
import com.miva.maintenance.model.RequestStatus;
import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.ServiceRequest;
import com.miva.maintenance.model.User;
import com.miva.maintenance.repository.RequestCategoryRepository;
import com.miva.maintenance.repository.ServiceRequestRepository;
import com.miva.maintenance.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Reports")
public class ReportController {

    private final ServiceRequestRepository requestRepository;
    private final RequestCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /** Summary counts used to power the Admin Dashboard's Reports tab. */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        List<ServiceRequest> all = requestRepository.findAll();

        Map<RequestStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(ServiceRequest::getStatus, Collectors.counting()));

        Map<String, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(RequestCategory::getId, RequestCategory::getName));

        Map<String, Long> byCategoryId = all.stream()
                .collect(Collectors.groupingBy(ServiceRequest::getCategoryId, Collectors.counting()));

        Map<String, Long> byCategoryName = new LinkedHashMap<>();
        byCategoryId.forEach((catId, count) -> byCategoryName.put(categoryNames.getOrDefault(catId, catId), count));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRequests", all.size());
        result.put("byStatus", byStatus);
        result.put("byCategory", byCategoryName);
        result.put("totalUsers", userRepository.count());
        result.put("totalStudents", userRepository.countByRole(Role.STUDENT_STAFF));
        result.put("totalOfficers", userRepository.countByRole(Role.OFFICER));
        result.put("totalAdmins", userRepository.countByRole(Role.ADMIN));

        return ResponseEntity.ok(result);
    }

    /** Exports all service requests as a downloadable CSV file (optionally filtered by status). */
    @GetMapping("/export")
    public void export(HttpServletResponse response, @RequestParam(required = false) RequestStatus status) throws IOException {
        List<ServiceRequest> requests = status != null
                ? requestRepository.findAll().stream().filter(r -> r.getStatus() == status).toList()
                : requestRepository.findAll();

        Map<String, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(RequestCategory::getId, RequestCategory::getName));

        Map<String, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=service_requests_report.csv");

        PrintWriter writer = response.getWriter();
        writer.println("ID,Title,Category,Location,Priority,Status,Submitted By,Assigned To,Created At");

        for (ServiceRequest r : requests) {
            writer.println(String.join(",",
                    csv(r.getId()),
                    csv(r.getTitle()),
                    csv(categoryNames.getOrDefault(r.getCategoryId(), r.getCategoryId())),
                    csv(r.getLocation()),
                    csv(r.getPriority()),
                    csv(r.getStatus() != null ? r.getStatus().name() : ""),
                    csv(userNames.getOrDefault(r.getSubmittedBy(), r.getSubmittedBy())),
                    csv(r.getAssignedTo() != null ? userNames.getOrDefault(r.getAssignedTo(), r.getAssignedTo()) : ""),
                    csv(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "")
            ));
        }
        writer.flush();
    }

    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

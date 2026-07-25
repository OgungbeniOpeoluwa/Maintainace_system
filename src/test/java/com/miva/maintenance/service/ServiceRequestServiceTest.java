package com.miva.maintenance.service;

import com.miva.maintenance.dto.ServiceRequestDto;
import com.miva.maintenance.model.RequestStatus;
import com.miva.maintenance.model.ServiceRequest;
import com.miva.maintenance.repository.AssignmentRepository;
import com.miva.maintenance.repository.ServiceRequestRepository;
import com.miva.maintenance.repository.StatusLogRepository;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock private ServiceRequestRepository requestRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private StatusLogRepository statusLogRepository;
    @Mock private MongoTemplate mongoTemplate;

    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        service = new ServiceRequestService(requestRepository, assignmentRepository, statusLogRepository, mongoTemplate);
    }

    @Test
    void submitCreatesAPendingRequestAndWritesAnAuditLogEntry() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Broken tap in Room 12");
        dto.setCategoryId("cat-plumbing");
        dto.setLocation("Hostel Block C, Room 12");
        dto.setPriority("HIGH");
        dto.setDescription("Water leaking under the sink");

        when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest saved = invocation.getArgument(0);
            saved.setId("req-1");
            return saved;
        });

        ServiceRequest result = service.submit(dto, "student-1", null);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getSubmittedBy()).isEqualTo("student-1");
        assertThat(result.getTitle()).isEqualTo("Broken tap in Room 12");
        verify(statusLogRepository).save(any());
    }

    @Test
    void claimSucceedsWhenTheRequestIsStillUnassigned() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ServiceRequest.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(
                ServiceRequest.builder().id("req-1").status(RequestStatus.ASSIGNED).assignedTo("officer-1").build()
        ));

        ServiceRequest result = service.claim("req-1", "officer-1");

        assertThat(result.getId()).isEqualTo("req-1");
        verify(assignmentRepository).save(any());
        verify(statusLogRepository).save(any());
    }

    @Test
    void claimFailsWhenAnotherOfficerAlreadyClaimedItFirst() {
        // modifiedCount = 0 simulates: the atomic update matched nothing because someone else
        // already claimed it between this officer loading the list and clicking "Claim".
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ServiceRequest.class)))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));

        assertThatThrownBy(() -> service.claim("req-1", "officer-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer available");
    }
}

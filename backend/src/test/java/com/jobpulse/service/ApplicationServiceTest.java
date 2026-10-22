package com.jobpulse.service;

import com.jobpulse.dto.application.ApplicationDtos.CreateApplicationRequest;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.Company;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import com.jobpulse.exception.BusinessRuleException;
import com.jobpulse.exception.DuplicateResourceException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests with mocked repositories: they check the service's decisions
 * (which exception, which status) without a database. The integration tests
 * cover the same flows end to end.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock JobApplicationRepository applicationRepository;
    @Mock JobRepository jobRepository;
    @Mock UserRepository userRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
    private ApplicationService service;

    private final UUID candidateId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private User candidate;
    private Job job;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applicationRepository, jobRepository, userRepository, new ApplicationWorkflow(), clock);
        candidate = new User("c@x.com", "hash", "Candidate", Role.CANDIDATE);
        ReflectionTestUtils.setField(candidate, "id", candidateId);
        job = new Job();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTitle("Engineer");
        job.setOrigin(Job.Origin.EMPLOYER);
        job.setCompany(new Company("Acme", "acme"));
    }

    @Test
    void applyingToMissingJobIs404() {
        when(jobRepository.findWithCompanyById(jobId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply(candidateId, new CreateApplicationRequest(jobId, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void applyingToClosedJobBreaksABusinessRule() {
        job.setStatus(Job.JobStatus.CLOSED);
        when(jobRepository.findWithCompanyById(jobId)).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> service.apply(candidateId, new CreateApplicationRequest(jobId, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void secondApplicationIsADuplicate() {
        when(jobRepository.findWithCompanyById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.getReferenceById(candidateId)).thenReturn(candidate);
        when(applicationRepository.findByUserIdAndJobId(candidateId, jobId))
                .thenReturn(Optional.of(new JobApplication(candidate, job, Instant.now(clock))));

        assertThatThrownBy(() -> service.apply(candidateId, new CreateApplicationRequest(jobId, "Hi")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void newApplicationStartsAsAppliedAtTheClockTime() {
        when(jobRepository.findWithCompanyById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.getReferenceById(candidateId)).thenReturn(candidate);
        when(applicationRepository.findByUserIdAndJobId(candidateId, jobId)).thenReturn(Optional.empty());
        when(applicationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.apply(candidateId, new CreateApplicationRequest(jobId, "  Excited to apply  "));

        assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.appliedAt()).isEqualTo(Instant.now(clock));
        assertThat(result.coverLetter()).isEqualTo("Excited to apply");
        assertThat(result.candidateManagesStatus()).isFalse();
        assertThat(result.allowedNextStatuses()).containsExactly(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void otherUsersApplicationLooksLikeItDoesNotExist() {
        User someoneElse = new User("o@x.com", "hash", "Other", Role.CANDIDATE);
        ReflectionTestUtils.setField(someoneElse, "id", UUID.randomUUID());
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findWithDetailsById(applicationId))
                .thenReturn(Optional.of(new JobApplication(someoneElse, job, Instant.now(clock))));

        assertThatThrownBy(() -> service.get(candidateId, applicationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

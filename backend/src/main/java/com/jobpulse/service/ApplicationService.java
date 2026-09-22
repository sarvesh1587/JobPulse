package com.jobpulse.service;

import com.jobpulse.dto.ApplicationRequest;
import com.jobpulse.dto.ApplicationResponse;
import com.jobpulse.dto.ApplicationUpdateRequest;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.entity.User;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.MatchScoreRepository;
import com.jobpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final MatchScoreRepository matchScoreRepository;

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listForUser(UUID userId) {
        return applicationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Saves a job into the pipeline. If the user already has an application
     * record for this job (e.g. they saved it once already), this updates
     * that record instead of creating a duplicate — matches the DB's
     * (user_id, job_id) unique constraint.
     */
    @Transactional
    public ApplicationResponse createOrUpdate(UUID userId, ApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("No job found with id " + request.jobId()));

        JobApplication application = applicationRepository.findByUserIdAndJobId(userId, request.jobId())
                .orElseGet(() -> JobApplication.builder().user(user).job(job).build());

        JobApplication.Status status = request.status() == null
                ? (application.getStatus() != null ? application.getStatus() : JobApplication.Status.SAVED)
                : parseStatus(request.status());

        application.setStatus(status);
        if (status == JobApplication.Status.APPLIED && application.getAppliedAt() == null) {
            application.setAppliedAt(Instant.now());
        }
        if (request.nextAction() != null) application.setNextAction(request.nextAction());
        if (request.notes() != null) application.setNotes(request.notes());

        return toResponse(applicationRepository.save(application));
    }

    @Transactional
    public ApplicationResponse update(UUID userId, UUID applicationId, ApplicationUpdateRequest request) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("No application found with id " + applicationId));

        if (!application.getUser().getId().equals(userId)) {
            // Treat as not-found rather than forbidden — don't reveal that
            // another user's application id exists at all.
            throw new ResourceNotFoundException("No application found with id " + applicationId);
        }

        if (request.status() != null) {
            JobApplication.Status newStatus = parseStatus(request.status());
            application.setStatus(newStatus);
            if (newStatus == JobApplication.Status.APPLIED && application.getAppliedAt() == null) {
                application.setAppliedAt(Instant.now());
            }
        }
        if (request.appliedAt() != null) application.setAppliedAt(request.appliedAt());
        if (request.nextAction() != null) application.setNextAction(request.nextAction());
        if (request.notes() != null) application.setNotes(request.notes());

        return toResponse(applicationRepository.save(application));
    }

    private JobApplication.Status parseStatus(String raw) {
        try {
            return JobApplication.Status.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status '" + raw + "'. Must be one of: SAVED, PLANNING, APPLIED, ASSESSMENT, INTERVIEW, OFFER, REJECTED");
        }
    }

    private ApplicationResponse toResponse(JobApplication app) {
        var matchScore = matchScoreRepository.findByUserIdAndJobId(app.getUser().getId(), app.getJob().getId());
        return new ApplicationResponse(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getJob().getCompany().getName(),
                app.getJob().getLocation(),
                app.getStatus().name(),
                matchScore.map(com.jobpulse.entity.MatchScore::getOverallScore).orElse(null),
                app.getAppliedAt(),
                app.getNextAction(),
                app.getNotes(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}

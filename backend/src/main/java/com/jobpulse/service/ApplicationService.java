package com.jobpulse.service;

import com.jobpulse.dto.application.ApplicationDtos.ApplicationDetailResponse;
import com.jobpulse.dto.application.ApplicationDtos.ApplicationResponse;
import com.jobpulse.dto.application.ApplicationDtos.CreateApplicationRequest;
import com.jobpulse.dto.application.ApplicationDtos.UpdateApplicationRequest;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.entity.User;
import com.jobpulse.exception.BusinessRuleException;
import com.jobpulse.exception.DuplicateResourceException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.mapper.ApplicationMapper;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The candidate's side of applications.
 *
 * Ownership rule: a candidate can only see and change their own
 * applications. Asking for someone else's id returns 404, the same as an id
 * that doesn't exist, so the API never confirms that another user's
 * application exists.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationWorkflow workflow;
    private final Clock clock;

    @Transactional
    public ApplicationDetailResponse apply(UUID candidateId, CreateApplicationRequest request) {
        Job job = jobRepository.findWithCompanyById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("This job doesn't exist or has been removed."));
        if (!job.isOpen()) {
            throw new BusinessRuleException("This job is no longer accepting applications.");
        }
        User candidate = userRepository.getReferenceById(candidateId);
        String coverLetter = TextNormalizer.clean(request.coverLetter());

        JobApplication application = applicationRepository.findByUserIdAndJobId(candidateId, job.getId())
                .map(existing -> reapplyIfWithdrawn(existing, candidate, coverLetter))
                .orElseGet(() -> newApplication(candidate, job, coverLetter));

        log.info("Candidate {} applied to job {} ({})", candidateId, job.getId(), job.getOrigin());
        // Flush so audit timestamps on the new history row are set before mapping.
        return toDetail(applicationRepository.saveAndFlush(application));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(UUID candidateId, ApplicationStatus status) {
        return applicationRepository.findForCandidate(candidateId, status).stream()
                .map(ApplicationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse get(UUID candidateId, UUID applicationId) {
        return toDetail(findOwned(candidateId, applicationId));
    }

    @Transactional
    public ApplicationDetailResponse update(UUID candidateId, UUID applicationId, UpdateApplicationRequest request) {
        JobApplication application = findOwned(candidateId, applicationId);

        boolean changesPipeline = request.status() != null || request.interviewAt() != null;
        if (changesPipeline) {
            if (!candidateManagesStatus(application)) {
                throw new BusinessRuleException(
                        "The employer manages this application's status. You can withdraw it instead.");
            }
            ApplicationStatus next = request.status() != null ? request.status() : application.getStatus();
            workflow.transition(application, next, request.interviewAt(), application.getUser(), null);
        }
        if (request.notes() != null) application.setNotes(TextNormalizer.clean(request.notes()));
        if (request.nextAction() != null) application.setNextAction(TextNormalizer.clean(request.nextAction()));

        return toDetail(applicationRepository.saveAndFlush(application));
    }

    @Transactional
    public ApplicationDetailResponse withdraw(UUID candidateId, UUID applicationId) {
        JobApplication application = findOwned(candidateId, applicationId);
        workflow.transition(application, ApplicationStatus.WITHDRAWN, null, application.getUser(), null);
        log.info("Candidate {} withdrew application {}", candidateId, applicationId);
        return toDetail(applicationRepository.saveAndFlush(application));
    }

    private JobApplication newApplication(User candidate, Job job, String coverLetter) {
        JobApplication application = new JobApplication(candidate, job, Instant.now(clock));
        application.setCoverLetter(coverLetter);
        application.recordInitialStatus(candidate);
        return application;
    }

    /** One row per (candidate, job), so re-applying after a withdrawal reopens the same row. */
    private JobApplication reapplyIfWithdrawn(JobApplication existing, User candidate, String coverLetter) {
        if (existing.getStatus() != ApplicationStatus.WITHDRAWN) {
            throw new DuplicateResourceException("You've already applied to this job.");
        }
        existing.changeStatus(ApplicationStatus.APPLIED, candidate, "Re-applied");
        existing.setAppliedAt(Instant.now(clock));
        existing.setInterviewAt(null);
        if (coverLetter != null) existing.setCoverLetter(coverLetter);
        return existing;
    }

    private JobApplication findOwned(UUID candidateId, UUID applicationId) {
        return applicationRepository.findWithDetailsById(applicationId)
                .filter(a -> a.belongsTo(candidateId))
                .orElseThrow(() -> new ResourceNotFoundException("Application not found."));
    }

    /** For jobs applied to elsewhere there's no employer here, so the candidate tracks the stages. */
    private boolean candidateManagesStatus(JobApplication application) {
        return !application.getJob().acceptsInAppApplications();
    }

    private ApplicationDetailResponse toDetail(JobApplication application) {
        boolean candidateManages = candidateManagesStatus(application);
        List<ApplicationStatus> allowed = candidateManages
                ? workflow.allowedNext(application)
                : application.getStatus().canMoveTo(ApplicationStatus.WITHDRAWN)
                    ? List.of(ApplicationStatus.WITHDRAWN) : List.of();
        return ApplicationMapper.toDetail(application, allowed, candidateManages);
    }
}

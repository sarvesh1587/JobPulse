package com.jobpulse.dto.application;

import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.Job;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Candidate-side application bodies (/api/applications). */
public final class ApplicationDtos {

    private ApplicationDtos() {}

    /**
     * For an EMPLOYER job this submits an application the employer will see.
     * For an INGESTED job it records "I applied on the company's site" so the
     * candidate can track it here. Either way it creates an APPLIED record.
     */
    public record CreateApplicationRequest(
            @NotNull UUID jobId,
            @Size(max = 5000) String coverLetter
    ) {}

    /**
     * Candidate edits. notes and nextAction are always allowed. status and
     * interviewAt can only be changed by the candidate for INGESTED jobs:
     * for EMPLOYER jobs the employer owns the pipeline, and the candidate
     * can only withdraw.
     */
    public record UpdateApplicationRequest(
            ApplicationStatus status,
            Instant interviewAt,
            @Size(max = 5000) String notes,
            @Size(max = 300) String nextAction
    ) {}

    public record ApplicationJob(
            UUID id,
            String title,
            UUID companyId,
            String companyName,
            String location,
            Job.Origin origin,
            Job.JobStatus status
    ) {}

    public record ApplicationResponse(
            UUID id,
            ApplicationJob job,
            ApplicationStatus status,
            Instant appliedAt,
            Instant interviewAt,
            String nextAction,
            Instant updatedAt
    ) {}

    /** Who made a status change, from the candidate's point of view. */
    public enum Actor { YOU, EMPLOYER, SYSTEM }

    public record StatusChangeResponse(
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            Actor changedBy,
            String note,
            Instant at
    ) {}

    public record ApplicationDetailResponse(
            UUID id,
            ApplicationJob job,
            ApplicationStatus status,
            Instant appliedAt,
            Instant interviewAt,
            String coverLetter,
            String notes,
            String nextAction,
            List<ApplicationStatus> allowedNextStatuses,
            boolean candidateManagesStatus,
            List<StatusChangeResponse> history,
            Instant updatedAt
    ) {}
}

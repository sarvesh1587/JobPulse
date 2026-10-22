package com.jobpulse.mapper;

import com.jobpulse.dto.application.ApplicationDtos.Actor;
import com.jobpulse.dto.application.ApplicationDtos.ApplicationDetailResponse;
import com.jobpulse.dto.application.ApplicationDtos.ApplicationJob;
import com.jobpulse.dto.application.ApplicationDtos.ApplicationResponse;
import com.jobpulse.dto.application.ApplicationDtos.StatusChangeResponse;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.ApplicationStatusChange;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;

import java.util.List;
import java.util.UUID;

public final class ApplicationMapper {

    private ApplicationMapper() {}

    public static ApplicationResponse toResponse(JobApplication a) {
        return new ApplicationResponse(a.getId(), toJob(a.getJob()), a.getStatus(), a.getAppliedAt(),
                a.getInterviewAt(), a.getNextAction(), a.getUpdatedAt());
    }

    public static ApplicationDetailResponse toDetail(JobApplication a, List<ApplicationStatus> allowedNext,
                                                     boolean candidateManagesStatus) {
        UUID candidateId = a.getUser().getId();
        return new ApplicationDetailResponse(
                a.getId(), toJob(a.getJob()), a.getStatus(), a.getAppliedAt(), a.getInterviewAt(),
                a.getCoverLetter(), a.getNotes(), a.getNextAction(), allowedNext, candidateManagesStatus,
                history(a, candidateId), a.getUpdatedAt());
    }

    /**
     * The audit trail labelled from the candidate's side: their own changes
     * read "YOU", anyone else's read "EMPLOYER". Only the id of changedBy is
     * read, which a Hibernate proxy can answer without loading the user.
     */
    public static List<StatusChangeResponse> history(JobApplication a, UUID candidateId) {
        return a.getHistory().stream().map(change -> toChange(change, candidateId)).toList();
    }

    private static StatusChangeResponse toChange(ApplicationStatusChange c, UUID candidateId) {
        Actor actor = c.getChangedBy() == null ? Actor.SYSTEM
                : c.getChangedBy().getId().equals(candidateId) ? Actor.YOU : Actor.EMPLOYER;
        return new StatusChangeResponse(c.getFromStatus(), c.getToStatus(), actor, c.getNote(), c.getCreatedAt());
    }

    private static ApplicationJob toJob(Job job) {
        return new ApplicationJob(job.getId(), job.getTitle(), job.getCompany().getId(),
                job.getCompany().getName(), job.getLocation(), job.getOrigin(), job.getStatus());
    }
}

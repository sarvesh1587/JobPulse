package com.jobpulse.dto.job;

import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.Job;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Job response bodies. Requests for employer-posted jobs live in EmployerDtos. */
public final class JobDtos {

    private JobDtos() {}

    public record CompanyRef(UUID id, String name) {}

    public record Salary(BigDecimal min, BigDecimal max, String currency) {}

    /**
     * One row in a result list. matchScore and saved are only filled in for
     * a signed-in candidate; for everyone else they are null and false.
     */
    public record JobSummaryResponse(
            UUID id,
            String title,
            CompanyRef company,
            String location,
            Job.WorkMode workMode,
            Job.EmploymentType employmentType,
            Job.ExperienceLevel experienceLevel,
            Salary salary,
            List<String> skills,
            Job.Origin origin,
            Instant postedAt,
            BigDecimal matchScore,
            boolean saved
    ) {}

    /** The candidate's own application for this job, if there is one. */
    public record MyApplication(UUID id, ApplicationStatus status, Instant appliedAt) {}

    public record JobDetailResponse(
            UUID id,
            String title,
            CompanyRef company,
            String companyDescription,
            String companyWebsite,
            String location,
            Job.WorkMode workMode,
            Job.EmploymentType employmentType,
            Job.ExperienceLevel experienceLevel,
            Salary salary,
            List<String> skills,
            String description,
            Job.Origin origin,
            Job.JobStatus status,
            String applicationUrl,
            Instant postedAt,
            Instant lastVerifiedAt,
            boolean saved,
            MyApplication myApplication
    ) {}

    public record MatchScoreResponse(
            UUID jobId,
            BigDecimal overallScore,
            BigDecimal skillScore,
            BigDecimal experienceScore,
            BigDecimal educationScore,
            BigDecimal locationScore,
            BigDecimal freshnessScore,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> concerns,
            List<String> positiveSignals
    ) {}
}

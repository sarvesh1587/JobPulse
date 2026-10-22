package com.jobpulse.mapper;

import com.jobpulse.dto.job.JobDtos.CompanyRef;
import com.jobpulse.dto.job.JobDtos.JobDetailResponse;
import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.dto.job.JobDtos.MatchScoreResponse;
import com.jobpulse.dto.job.JobDtos.MyApplication;
import com.jobpulse.dto.job.JobDtos.Salary;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.matching.MatchBreakdown;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Plain, explicit entity-to-DTO mapping. A library like MapStruct would also
 * work, but hand-written mapping is easy to read and debug at this size.
 * Mappers take already-loaded data (skills, scores) as arguments and never
 * run queries themselves.
 */
public final class JobMapper {

    private JobMapper() {}

    public static JobSummaryResponse toSummary(Job job, List<String> skills, BigDecimal matchScore, boolean saved) {
        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                companyRef(job),
                job.getLocation(),
                job.getWorkMode(),
                job.getEmploymentType(),
                job.getExperienceLevel(),
                salary(job),
                skills,
                job.getOrigin(),
                job.getPostedAt(),
                matchScore,
                saved);
    }

    public static JobDetailResponse toDetail(Job job, List<String> skills, boolean saved, JobApplication myApplication) {
        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                companyRef(job),
                job.getCompany().getDescription(),
                job.getCompany().getWebsite(),
                job.getLocation(),
                job.getWorkMode(),
                job.getEmploymentType(),
                job.getExperienceLevel(),
                salary(job),
                skills,
                job.getDescription(),
                job.getOrigin(),
                job.getStatus(),
                job.getApplicationUrl(),
                job.getPostedAt(),
                job.getLastVerifiedAt(),
                saved,
                myApplication == null ? null
                        : new MyApplication(myApplication.getId(), myApplication.getStatus(), myApplication.getAppliedAt()));
    }

    public static MatchScoreResponse toMatchScore(UUID jobId, MatchBreakdown b) {
        return new MatchScoreResponse(jobId, b.overallScore(), b.skillScore(), b.experienceScore(),
                b.educationScore(), b.locationScore(), b.freshnessScore(),
                b.matchedSkills(), b.missingSkills(), b.concerns(), b.positiveSignals());
    }

    private static CompanyRef companyRef(Job job) {
        return new CompanyRef(job.getCompany().getId(), job.getCompany().getName());
    }

    private static Salary salary(Job job) {
        if (job.getSalaryMin() == null && job.getSalaryMax() == null) return null;
        return new Salary(job.getSalaryMin(), job.getSalaryMax(), job.getSalaryCurrency());
    }
}

package com.jobpulse.mapper;

import com.jobpulse.dto.JobDetailResponse;
import com.jobpulse.dto.JobSummaryResponse;
import com.jobpulse.entity.Job;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class JobMapper {

    public JobSummaryResponse toSummary(Job job, List<String> skills, BigDecimal matchScore) {
        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation(),
                job.getEmploymentType() != null ? job.getEmploymentType().name() : null,
                job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null,
                skills,
                job.getStatus().name(),
                job.getPostedAt(),
                matchScore
        );
    }

    public JobDetailResponse toDetail(Job job, List<String> skills, BigDecimal matchScore) {
        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation(),
                job.getDescription(),
                job.getEmploymentType() != null ? job.getEmploymentType().name() : null,
                job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null,
                skills,
                job.getApplicationUrl(),
                job.getSourceUrl(),
                job.getStatus().name(),
                job.getPostedAt(),
                job.getLastVerifiedAt(),
                matchScore
        );
    }
}

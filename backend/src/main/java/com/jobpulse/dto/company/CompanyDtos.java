package com.jobpulse.dto.company;

import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Public company pages (/api/companies). Every number is derived from currently active listings. */
public final class CompanyDtos {

    private CompanyDtos() {}

    public record CompanySummaryResponse(
            UUID id,
            String name,
            String headquarters,
            String website,
            long openRoles,
            long internships,
            long entryLevel
    ) {}

    public record CompanyDetailResponse(
            UUID id,
            String name,
            String description,
            String website,
            String headquarters,
            String careersUrl,
            int openRoles,
            List<String> locations,
            List<String> topSkills,
            Map<String, Long> rolesByEmploymentType,
            Map<String, Long> rolesByExperienceLevel,
            Instant lastVerifiedAt,
            List<JobSummaryResponse> openJobs
    ) {}
}

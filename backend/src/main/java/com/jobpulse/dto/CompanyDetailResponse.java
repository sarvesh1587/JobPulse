package com.jobpulse.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CompanyDetailResponse(
        UUID id,
        String name,
        String careersUrl,
        int openRoles,
        int internships,
        int entryLevel,
        int experienced,
        List<String> locations,
        List<String> topSkills,
        Map<String, Long> roleDistributionByEmploymentType,
        Map<String, Long> roleDistributionByExperienceLevel,
        Instant lastVerifiedAt,
        String historicalHiringTrend, // always "Not enough data yet" — placeholder, never fabricated
        List<JobSummaryResponse> openJobs
) {}

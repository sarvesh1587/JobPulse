package com.jobpulse.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobDetailResponse(
        UUID id,
        String title,
        String company,
        String location,
        String description,
        String employmentType,
        String experienceLevel,
        List<String> skills,
        String applicationUrl,
        String sourceUrl,
        String status,
        Instant postedAt,
        Instant lastVerifiedAt,
        BigDecimal matchScore
) {}

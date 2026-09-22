package com.jobpulse.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobSummaryResponse(
        UUID id,
        String title,
        String company,
        String location,
        String employmentType,
        String experienceLevel,
        List<String> skills,
        String status,
        Instant postedAt,
        BigDecimal matchScore // null when no candidate context is available
) {}

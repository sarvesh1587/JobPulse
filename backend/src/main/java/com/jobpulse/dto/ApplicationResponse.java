package com.jobpulse.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        String company,
        String location,
        String status,
        BigDecimal matchScore,   // null if no match score has been computed yet
        Instant appliedAt,
        String nextAction,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}

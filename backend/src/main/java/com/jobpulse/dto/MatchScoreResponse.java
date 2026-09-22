package com.jobpulse.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        List<String> positiveSignals,
        Instant computedAt
) {}

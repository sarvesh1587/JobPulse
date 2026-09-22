package com.jobpulse.matching;

import java.math.BigDecimal;
import java.util.List;

public record MatchBreakdown(
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

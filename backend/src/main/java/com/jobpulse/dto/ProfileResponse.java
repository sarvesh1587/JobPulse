package com.jobpulse.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String fullName,
        String email,
        String educationLevel,
        Integer graduationYear,
        String location,
        List<String> preferredRoles,
        BigDecimal experienceYears,
        String summary,
        List<String> skills
) {}

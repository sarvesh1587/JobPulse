package com.jobpulse.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProfileRequest(
        String educationLevel,
        Integer graduationYear,
        String location,
        List<String> preferredRoles,
        BigDecimal experienceYears,
        String summary,
        List<String> skills
) {}

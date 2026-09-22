package com.jobpulse.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanySummaryResponse(
        UUID id,
        String name,
        String careersUrl,
        int openRoles,
        int internships,
        int entryLevel,
        List<String> locations,
        List<String> topSkills,
        Instant lastVerifiedAt // null when there are no active listings to verify against
) {}

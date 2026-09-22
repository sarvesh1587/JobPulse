package com.jobpulse.dto;

import java.time.Instant;
import java.util.List;

public record SkillGapResponse(
        List<String> targetRoles,      // what this was computed against — profile.preferredRoles, or empty if a general scan was used
        int jobsAnalyzed,
        List<String> readySkills,      // candidate's own skills that are actually in demand among the analyzed jobs
        List<SkillGapItemResponse> priorityGaps,
        Instant computedAt
) {}

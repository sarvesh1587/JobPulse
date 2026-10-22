package com.jobpulse.dto.skillgap;

import java.time.Instant;
import java.util.List;

public record SkillGapResponse(
        List<String> targetRoles,   // profile.preferredRoles; empty means all recent jobs were used
        int jobsAnalyzed,
        List<String> readySkills,   // the candidate's skills that are in demand among those jobs
        List<GapItem> priorityGaps,
        Instant computedAt
) {
    public record GapItem(
            String skillName,
            int priority,               // 1 = the most common missing skill
            int frequencyInTargetRoles, // how many analysed jobs ask for it
            int percentOfJobs
    ) {}
}

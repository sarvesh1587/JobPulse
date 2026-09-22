package com.jobpulse.dto;

public record SkillGapItemResponse(
        String skillName,
        int priority,                // 1 = most impactful gap to close first
        int frequencyInTargetRoles    // how many analyzed job postings required this skill
) {}

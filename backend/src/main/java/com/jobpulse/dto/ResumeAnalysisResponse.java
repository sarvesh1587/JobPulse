package com.jobpulse.dto;

import java.util.List;
import java.util.UUID;

public record ResumeAnalysisResponse(
        List<String> detectedSkills,
        String likelyEducationLevel,
        Integer likelyGraduationYear,
        String extractedTextPreview, // first ~500 chars, so the user can sanity-check extraction quality
        boolean persisted,           // false when consentToStore was not given — nothing was saved
        UUID resumeId                // null when not persisted
) {}

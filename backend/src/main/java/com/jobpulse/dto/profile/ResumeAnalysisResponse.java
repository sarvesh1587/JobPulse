package com.jobpulse.dto.profile;

import java.util.List;
import java.util.UUID;

public record ResumeAnalysisResponse(
        List<String> detectedSkills,
        String likelyEducationLevel,
        Integer likelyGraduationYear,
        String extractedTextPreview, // first ~500 characters, so the candidate can sanity-check extraction
        boolean persisted,           // false when consentToStore wasn't given: nothing was saved
        UUID resumeId,               // null when not persisted
        List<String> skillsAddedToProfile
) {}

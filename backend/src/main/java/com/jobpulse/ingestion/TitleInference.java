package com.jobpulse.ingestion;

/**
 * None of the three ATS platforms expose a clean "employment type" or
 * "experience level" field consistently, so every adapter falls back to
 * reading the job title. Centralized here so the heuristic lives in exactly
 * one place instead of being copy-pasted per adapter.
 */
final class TitleInference {

    private TitleInference() {}

    static NormalizedJob.EmploymentType employmentType(String title) {
        if (title == null) return NormalizedJob.EmploymentType.UNKNOWN;
        String t = title.toLowerCase();
        if (t.contains("intern")) return NormalizedJob.EmploymentType.INTERNSHIP;
        if (t.contains("contract") || t.contains("freelance")) return NormalizedJob.EmploymentType.CONTRACT;
        return NormalizedJob.EmploymentType.FULL_TIME;
    }

    static NormalizedJob.ExperienceLevel experienceLevel(String title) {
        if (title == null) return NormalizedJob.ExperienceLevel.UNKNOWN;
        String t = title.toLowerCase();
        if (t.contains("intern")) return NormalizedJob.ExperienceLevel.INTERN;
        if (t.contains("senior") || t.contains("staff") || t.contains("principal") || t.contains("lead"))
            return NormalizedJob.ExperienceLevel.SENIOR;
        if (t.contains("junior") || t.contains("entry") || t.contains("graduate") || t.matches(".*\\bi\\b.*"))
            return NormalizedJob.ExperienceLevel.ENTRY;
        return NormalizedJob.ExperienceLevel.UNKNOWN;
    }
}

package com.jobpulse.ingestion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The shape every {@link JobSourceAdapter} normalizes its source-specific
 * response into, before it reaches validation/dedup/upsert. Nothing
 * downstream of this record should know or care which ATS a job came from.
 */
public record NormalizedJob(
        String sourceName,          // GREENHOUSE | LEVER | ASHBY
        String externalId,          // the source's own job id — used as the primary dedup key
        String companyName,
        String title,
        String description,
        String location,
        EmploymentType employmentType,
        ExperienceLevel experienceLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String applicationUrl,
        String sourceUrl,
        Instant postedAt,
        List<String> rawSkillHints    // free-text tokens an adapter could pull from tags/departments; best-effort only
) {
    public enum EmploymentType { INTERNSHIP, FULL_TIME, CONTRACT, UNKNOWN }
    public enum ExperienceLevel { INTERN, ENTRY, MID, SENIOR, UNKNOWN }
}

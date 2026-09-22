package com.jobpulse.ingestion;

import java.util.List;

/**
 * Contract for pulling jobs from one ATS's public job-board API and
 * normalizing them into {@link NormalizedJob}. Adding a new source means
 * writing one more implementation of this interface — nothing else in the
 * ingestion pipeline needs to change.
 */
public interface JobSourceAdapter {

    /** Matches the "name" column in job_sources, e.g. "GREENHOUSE". */
    String sourceName();

    /**
     * Fetches and normalizes every currently published job for one company.
     *
     * @param companySlug the company's slug/token on this ATS (e.g. "stripe")
     * @return normalized jobs — never null; empty list if the board has none
     * @throws JobSourceFetchException if the request fails or the response
     *         can't be parsed. Callers must not let one failing company
     *         board abort ingestion for every other company.
     */
    List<NormalizedJob> fetchJobs(String companySlug);
}

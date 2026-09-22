package com.jobpulse.ingestion;

/** Summary of one adapter run against one company board — for logging/observability. */
public record IngestionResult(
        String source,
        String companySlug,
        int fetched,
        int created,
        int updated,
        int skippedInvalid,
        String error // null on success
) {
    public static IngestionResult failed(String source, String slug, String error) {
        return new IngestionResult(source, slug, 0, 0, 0, 0, error);
    }
}

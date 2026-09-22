package com.jobpulse.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates one ingestion run: FETCH (adapter) -> NORMALIZE (adapter) ->
 * VALIDATE -> DEDUPLICATE -> UPSERT -> VERIFY. The dedup/upsert/verify steps
 * live in JobUpsertService so @Transactional applies correctly (see its
 * class comment — self-invocation would otherwise bypass Spring's proxy).
 *
 * Deduplication follows the brief's priority order:
 *   1. external job id, scoped to source (cheapest, most reliable)
 *   2. normalized company + normalized title + normalized location
 * Description-similarity matching (the brief's further fallback) needs the
 * embeddings the Python AI service will provide — not implemented here.
 */
@Slf4j
@Service
public class IngestionService {

    private final Map<String, JobSourceAdapter> adaptersByName;
    private final JobUpsertService jobUpsertService;

    public IngestionService(List<JobSourceAdapter> adapters, JobUpsertService jobUpsertService) {
        this.adaptersByName = adapters.stream()
                .collect(Collectors.toMap(JobSourceAdapter::sourceName, a -> a));
        this.jobUpsertService = jobUpsertService;
    }

    public List<IngestionResult> runAll(List<IngestionProperties.Target> targets) {
        return targets.stream().map(this::runOne).toList();
    }

    public IngestionResult runOne(IngestionProperties.Target target) {
        JobSourceAdapter adapter = adaptersByName.get(target.source().toUpperCase(Locale.ROOT));
        if (adapter == null) {
            return IngestionResult.failed(target.source(), target.slug(), "No adapter registered for source");
        }

        List<NormalizedJob> fetched;
        try {
            fetched = adapter.fetchJobs(target.slug());
        } catch (JobSourceFetchException e) {
            log.warn("Ingestion fetch failed for {} / {}: {}", target.source(), target.slug(), e.getMessage());
            return IngestionResult.failed(target.source(), target.slug(), e.getMessage());
        }

        int created = 0, updated = 0, skipped = 0;
        for (NormalizedJob nj : fetched) {
            if (!isValid(nj)) {
                skipped++;
                continue;
            }
            try {
                boolean wasNew = jobUpsertService.upsert(nj);
                if (wasNew) created++; else updated++;
            } catch (Exception e) {
                // One bad record should never abort the whole board's ingestion.
                log.warn("Skipping one job from {} / {} due to upsert error: {}",
                        target.source(), target.slug(), e.getMessage());
                skipped++;
            }
        }

        log.info("Ingested {} / {}: {} fetched, {} created, {} updated, {} skipped",
                target.source(), target.slug(), fetched.size(), created, updated, skipped);

        return new IngestionResult(target.source(), target.slug(), fetched.size(), created, updated, skipped, null);
    }

    private boolean isValid(NormalizedJob job) {
        return job.externalId() != null && !job.externalId().isBlank()
                && job.title() != null && !job.title().isBlank()
                && job.applicationUrl() != null && !job.applicationUrl().isBlank();
    }
}

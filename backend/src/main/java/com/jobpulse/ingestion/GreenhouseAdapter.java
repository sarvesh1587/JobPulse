package com.jobpulse.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Greenhouse's public "Job Board API" — no auth required.
 * GET https://boards-api.greenhouse.io/v1/boards/{slug}/jobs?content=true
 */
@Component
@RequiredArgsConstructor
public class GreenhouseAdapter implements JobSourceAdapter {

    private static final String URL_TEMPLATE =
            "https://boards-api.greenhouse.io/v1/boards/{slug}/jobs?content=true";

    private final RestClient ingestionRestClient;

    @Override
    public String sourceName() {
        return "GREENHOUSE";
    }

    @Override
    public List<NormalizedJob> fetchJobs(String companySlug) {
        JsonNode root;
        try {
            root = ingestionRestClient.get()
                    .uri(URL_TEMPLATE, companySlug)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new JobSourceFetchException(
                    "Greenhouse fetch failed for board '" + companySlug + "'", e);
        }

        if (root == null || !root.has("jobs")) {
            return List.of();
        }

        List<NormalizedJob> results = new ArrayList<>();
        for (JsonNode job : root.get("jobs")) {
            results.add(normalize(job, companySlug));
        }
        return results;
    }

    private NormalizedJob normalize(JsonNode job, String companySlug) {
        String title = text(job, "title");
        String description = job.path("content").isMissingNode() ? null : job.path("content").asText(null);
        String location = job.path("location").path("name").asText(null);
        String absoluteUrl = text(job, "absolute_url");
        String externalId = job.path("id").asText(null);
        Instant postedAt = parseInstant(text(job, "updated_at"));

        return new NormalizedJob(
                sourceName(),
                externalId,
                companySlug,
                title,
                description,
                location,
                TitleInference.employmentType(title),
                TitleInference.experienceLevel(title),
                null,
                null,
                absoluteUrl,
                absoluteUrl,
                postedAt,
                List.of()
        );
    }

    private String text(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? null : child.asText();
    }

    private Instant parseInstant(String raw) {
        if (raw == null) return null;
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

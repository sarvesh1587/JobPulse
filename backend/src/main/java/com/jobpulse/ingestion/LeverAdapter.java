package com.jobpulse.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lever's public Postings API — no auth required.
 * GET https://api.lever.co/v0/postings/{slug}?mode=json
 * Returns a bare JSON array (not wrapped in an object), unlike Greenhouse/Ashby.
 */
@Component
@RequiredArgsConstructor
public class LeverAdapter implements JobSourceAdapter {

    private static final String URL_TEMPLATE =
            "https://api.lever.co/v0/postings/{slug}?mode=json";

    private final RestClient ingestionRestClient;

    @Override
    public String sourceName() {
        return "LEVER";
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
                    "Lever fetch failed for board '" + companySlug + "'", e);
        }

        if (root == null || !root.isArray()) {
            return List.of();
        }

        List<NormalizedJob> results = new ArrayList<>();
        for (JsonNode job : root) {
            results.add(normalize(job, companySlug));
        }
        return results;
    }

    private NormalizedJob normalize(JsonNode job, String companySlug) {
        String title = text(job, "text"); // Lever calls the title "text"
        String description = job.path("descriptionPlain").isMissingNode()
                ? job.path("description").asText(null)
                : job.path("descriptionPlain").asText(null);
        String location = job.path("categories").path("location").asText(null);
        String commitment = job.path("categories").path("commitment").asText(null); // "Full-time" / "Internship"
        String hostedUrl = text(job, "hostedUrl");
        String applyUrl = job.path("applyUrl").isMissingNode() ? hostedUrl : job.path("applyUrl").asText(hostedUrl);
        String externalId = text(job, "id");

        Instant postedAt = job.path("createdAt").isMissingNode()
                ? null
                : Instant.ofEpochMilli(job.path("createdAt").asLong());

        return new NormalizedJob(
                sourceName(),
                externalId,
                companySlug,
                title,
                description,
                location,
                commitment != null && commitment.toLowerCase().contains("intern")
                        ? NormalizedJob.EmploymentType.INTERNSHIP
                        : TitleInference.employmentType(title),
                TitleInference.experienceLevel(title),
                null,
                null,
                applyUrl,
                hostedUrl,
                postedAt,
                List.of()
        );
    }

    private String text(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? null : child.asText();
    }
}

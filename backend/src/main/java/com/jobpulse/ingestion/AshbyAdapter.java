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
 * Ashby's public Job Board API — no auth required.
 * GET https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true
 */
@Component
@RequiredArgsConstructor
public class AshbyAdapter implements JobSourceAdapter {

    private static final String URL_TEMPLATE =
            "https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true";

    private final RestClient ingestionRestClient;

    @Override
    public String sourceName() {
        return "ASHBY";
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
                    "Ashby fetch failed for board '" + companySlug + "'", e);
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
        String description = job.path("descriptionPlain").isMissingNode()
                ? null
                : job.path("descriptionPlain").asText(null);
        String location = text(job, "location");
        String applyUrl = job.path("applyUrl").isMissingNode()
                ? text(job, "jobUrl")
                : job.path("applyUrl").asText();
        String jobUrl = text(job, "jobUrl");
        String externalId = text(job, "id");
        String ashbyEmploymentType = text(job, "employmentType"); // FullTime | Intern | Contract | PartTime

        Instant postedAt = parseInstant(text(job, "publishedAt"));

        return new NormalizedJob(
                sourceName(),
                externalId,
                companySlug,
                title,
                description,
                location,
                mapEmploymentType(ashbyEmploymentType, title),
                TitleInference.experienceLevel(title),
                null,
                null,
                applyUrl != null ? applyUrl : jobUrl,
                jobUrl,
                postedAt,
                List.of()
        );
    }

    private NormalizedJob.EmploymentType mapEmploymentType(String ashbyType, String title) {
        if (ashbyType == null) return TitleInference.employmentType(title);
        return switch (ashbyType) {
            case "Intern" -> NormalizedJob.EmploymentType.INTERNSHIP;
            case "Contract" -> NormalizedJob.EmploymentType.CONTRACT;
            case "FullTime", "PartTime" -> NormalizedJob.EmploymentType.FULL_TIME;
            default -> TitleInference.employmentType(title);
        };
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

package com.jobpulse.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds jobpulse.ingestion.* from application.yml — which companies to pull
 * from which source, and how the scheduler should behave. Adding a company
 * to track is a config change, not a code change.
 */
@ConfigurationProperties(prefix = "jobpulse.ingestion")
public record IngestionProperties(
        boolean enabled,
        long fixedDelayMinutes,
        List<Target> targets
) {
    public record Target(String source, String slug) {}
}

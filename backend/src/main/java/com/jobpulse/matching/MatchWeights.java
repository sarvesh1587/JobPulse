package com.jobpulse.matching;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds jobpulse.matching.weights.* — kept configurable rather than
 * hardcoded so the weighting can be tuned without a code change. Only the
 * five dimensions this engine actually computes are used here; a
 * "job-type" weight exists in application.yml for future use once the
 * candidate has an explicit employment-type preference to compare against,
 * but isn't wired into scoring yet — see MatchingService's class comment.
 */
@ConfigurationProperties(prefix = "jobpulse.matching.weights")
public record MatchWeights(
        double skills,
        double experience,
        double education,
        double location,
        double freshness,
        double jobType
) {}

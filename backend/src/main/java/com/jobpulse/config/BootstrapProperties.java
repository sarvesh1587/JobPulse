package com.jobpulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jobpulse.bootstrap.*: the first admin account, read from environment
 * variables so no credentials live in source control.
 */
@ConfigurationProperties(prefix = "jobpulse.bootstrap")
public record BootstrapProperties(
        String adminEmail,
        String adminPassword
) {}

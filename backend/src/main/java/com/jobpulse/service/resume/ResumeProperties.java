package com.jobpulse.service.resume;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobpulse.resume")
public record ResumeProperties(
        String storageDir,
        long maxSizeBytes
) {}

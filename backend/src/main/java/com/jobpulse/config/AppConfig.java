package com.jobpulse.config;

import com.jobpulse.ingestion.IngestionProperties;
import com.jobpulse.matching.MatchWeights;
import com.jobpulse.security.AuthProperties;
import com.jobpulse.service.resume.ResumeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Wires the typed configuration records (jobpulse.* in application.yml),
 * JPA auditing (created_at/updated_at) and scheduling (ingestion).
 */
@Configuration
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({
        AuthProperties.class,
        ResumeProperties.class,
        MatchWeights.class,
        IngestionProperties.class,
        BootstrapProperties.class
})
public class AppConfig {

    /** Injected rather than calling Instant.now() directly, so time-based logic can be tested. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

package com.jobpulse.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionScheduler {

    private final IngestionService ingestionService;
    private final IngestionProperties properties;

    @Scheduled(
            initialDelayString = "1",
            fixedDelayString = "${jobpulse.ingestion.fixed-delay-minutes:30}",
            timeUnit = java.util.concurrent.TimeUnit.MINUTES
    )
    public void runScheduledIngestion() {
        if (!properties.enabled() || properties.targets() == null || properties.targets().isEmpty()) {
            return;
        }
        log.info("Starting scheduled ingestion run for {} target(s)", properties.targets().size());
        var results = ingestionService.runAll(properties.targets());
        long failures = results.stream().filter(r -> r.error() != null).count();
        if (failures > 0) {
            log.warn("{} of {} ingestion targets failed this run", failures, results.size());
        }
    }
}

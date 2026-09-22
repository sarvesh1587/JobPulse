package com.jobpulse.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * Shared client for talking to external ATS job-board APIs. Short,
     * explicit timeouts so one slow/unreachable source can't stall the
     * whole ingestion run, and an identifying User-Agent per good API
     * citizenship (see JobPulse's ingestion policy: never scrape
     * aggressively, always identify the client).
     */
    @Bean
    public RestClient ingestionRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(10));

        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "JobPulse-Ingestion/0.1 (+https://jobpulse.example.com)")
                .build();
    }
}

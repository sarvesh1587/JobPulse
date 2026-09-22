package com.jobpulse.config;

import com.jobpulse.matching.MatchWeights;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MatchWeights.class)
public class MatchingConfig {
}

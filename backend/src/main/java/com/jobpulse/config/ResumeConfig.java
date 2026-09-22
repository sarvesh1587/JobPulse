package com.jobpulse.config;

import com.jobpulse.resume.ResumeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResumeProperties.class)
public class ResumeConfig {
}

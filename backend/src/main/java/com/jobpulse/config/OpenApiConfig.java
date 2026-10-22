package com.jobpulse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobPulseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("JobPulse API")
                .version("1.0")
                .description("""
                        REST API for JobPulse. Authentication is a JWT in an HttpOnly cookie: call \
                        POST /api/auth/login from this page and later requests are authenticated. \
                        Write requests need the X-XSRF-TOKEN header, which Swagger UI adds from the \
                        XSRF-TOKEN cookie. Call GET /api/auth/csrf once first to get that cookie."""));
    }
}

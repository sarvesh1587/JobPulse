package com.jobpulse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the built React app from classpath:/static when it's bundled into
 * the jar (the single-image deployment, see the root Dockerfile). The SPA
 * and the API then share one origin, so the auth cookie stays first-party.
 *
 * Real files (/assets/*.js, /favicon) are served as-is. Any other non-API
 * path (/jobs/123, /dashboard) gets index.html, and React Router renders it.
 * /api/** is never rewritten, so unknown API paths still return a JSON 404.
 * In local development there is no bundled index.html, so nothing changes.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) return requested;
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) return null;
                        Resource index = location.createRelative("index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}

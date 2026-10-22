package com.jobpulse.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds jobpulse.auth.* from application.yml.
 *
 * @param jwtSecret     HMAC key for signing tokens; must be at least 32 bytes (256 bits)
 * @param tokenTtl      how long a sign-in lasts
 * @param cookieName    name of the HttpOnly cookie that carries the token
 * @param cookieSecure  true in production so the cookie is only sent over HTTPS
 */
@ConfigurationProperties(prefix = "jobpulse.auth")
public record AuthProperties(
        String jwtSecret,
        Duration tokenTtl,
        String cookieName,
        boolean cookieSecure
) {}

package com.jobpulse.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-01T10:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(3, clock);

    private int attempt(String ip, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void blocksAfterTheLimitWithinAMinute() throws Exception {
        for (int i = 0; i < 3; i++) assertThat(attempt("1.1.1.1", "/api/auth/login")).isEqualTo(200);
        assertThat(attempt("1.1.1.1", "/api/auth/login")).isEqualTo(429);
    }

    @Test
    void limitsArePerClientAndResetAfterTheWindow() throws Exception {
        for (int i = 0; i < 4; i++) attempt("1.1.1.1", "/api/auth/login");
        assertThat(attempt("2.2.2.2", "/api/auth/login")).isEqualTo(200);

        clock.advance(Duration.ofSeconds(61));
        assertThat(attempt("1.1.1.1", "/api/auth/login")).isEqualTo(200);
    }

    @Test
    void otherEndpointsAreNotLimited() throws Exception {
        for (int i = 0; i < 10; i++) assertThat(attempt("1.1.1.1", "/api/applications")).isEqualTo(200);
    }
}

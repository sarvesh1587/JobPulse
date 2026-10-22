package com.jobpulse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic brute-force protection for sign-in and registration: at most
 * {@code maxAttempts} requests per client IP per one-minute window, answered
 * with 429 after that.
 *
 * Known limitation: counters live in this JVM's memory, so they reset on
 * restart and aren't shared between instances. Running several instances
 * would need a shared store (Redis, for example) or a rate limit at the gateway.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/auth/register");
    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int maxAttempts;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(int maxAttempts, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.clock = clock;
    }

    private record Window(long startEpochSecond, int count) {}

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && LIMITED_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        long now = Instant.now(clock).getEpochSecond();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> now - w.startEpochSecond() >= WINDOW_SECONDS);
        }
        // remoteAddr is the real client IP: server.forward-headers-strategy=native
        // only trusts X-Forwarded-For from internal proxies (our nginx).
        Window window = windows.compute(request.getRemoteAddr(), (ip, current) ->
                current == null || now - current.startEpochSecond() >= WINDOW_SECONDS
                        ? new Window(now, 1)
                        : new Window(current.startEpochSecond(), current.count() + 1));

        if (window.count() > maxAttempts) {
            long retryAfter = WINDOW_SECONDS - (now - window.startEpochSecond());
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":429,"code":"TOO_MANY_REQUESTS","message":"Too many attempts. Wait a minute and try again."}""");
            return;
        }
        chain.doFilter(request, response);
    }
}

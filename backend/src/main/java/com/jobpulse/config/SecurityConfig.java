package com.jobpulse.config;

import com.jobpulse.repository.UserRepository;
import com.jobpulse.security.AuthCookieService;
import com.jobpulse.security.AuthRateLimitFilter;
import com.jobpulse.security.JsonSecurityErrorHandler;
import com.jobpulse.security.JwtAuthFilter;
import com.jobpulse.security.JwtService;
import com.jobpulse.security.SpaCsrfTokenRequestHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

/**
 * Who can call what.
 *
 * Authentication: a JWT in an HttpOnly cookie (see AuthCookieService), no
 * server session. Because the browser sends the cookie automatically, CSRF
 * protection stays on: Spring sets a readable XSRF-TOKEN cookie, and the SPA
 * echoes it back in an X-XSRF-TOKEN header on every write. Another site can
 * make the browser send our cookie but can't read the token to send the header.
 *
 * Authorization happens at two levels:
 *  1. URL rules below decide which role may reach an endpoint at all.
 *  2. Services check ownership ("is this your application / your job?").
 *     A role check alone can't do that.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           AuthCookieService authCookieService,
                                           UserRepository userRepository,
                                           JsonSecurityErrorHandler errorHandler,
                                           CorsConfigurationSource corsConfigurationSource,
                                           @Value("${jobpulse.auth.rate-limit-per-minute:10}") int rateLimit,
                                           Clock clock) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/docs/**", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/auth/me").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll()
                // Match scores are personal, so this rule must come before the public /api/jobs/* rule.
                .requestMatchers(HttpMethod.GET, "/api/jobs/*/match").hasRole("CANDIDATE")
                .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/*", "/api/companies", "/api/companies/*", "/api/skills").permitAll()
                .requestMatchers("/api/employer/**").hasRole("EMPLOYER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/saved-jobs/**", "/api/applications/**", "/api/profile/**",
                        "/api/dashboard", "/api/skill-gap").hasRole("CANDIDATE")
                .requestMatchers("/api/**", "/actuator/**").authenticated()
                // Everything else is the bundled React app (HTML, JS, CSS). Its pages
                // are public; the data they load is protected by the rules above.
                .anyRequest().permitAll())
            .addFilterBefore(new AuthRateLimitFilter(rateLimit, clock), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthFilter(jwtService, authCookieService, userRepository),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * In dev and Docker the SPA and API share one origin through a proxy, so
     * CORS mostly doesn't apply. This allows a separately hosted frontend
     * when CORS_ALLOWED_ORIGINS lists it.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${jobpulse.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** BCrypt: salted and deliberately slow, so leaked hashes are expensive to crack. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

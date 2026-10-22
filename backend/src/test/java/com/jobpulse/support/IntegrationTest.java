package com.jobpulse.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests. One real PostgreSQL server (embedded,
 * no Docker needed) starts per JVM, Flyway runs every migration against
 * it, and the full Spring context, security filters included, handles
 * requests through MockMvc. Tests create their own users with unique emails,
 * so they don't depend on each other or on run order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    public static final String PASSWORD = "correct-horse-battery";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.example.com";
    }

    /** Registers through the real endpoint and returns the auth cookie it set. */
    protected Cookie registerCandidate(String email) throws Exception {
        return register(Map.of("email", email, "password", PASSWORD, "fullName", "Test Candidate", "role", "CANDIDATE"));
    }

    protected Cookie registerEmployer(String email, String companyName) throws Exception {
        return register(Map.of("email", email, "password", PASSWORD, "fullName", "Test Recruiter",
                "role", "EMPLOYER", "companyName", companyName));
    }

    protected Cookie login(String email) throws Exception {
        return mvc.perform(withJson(post("/api/auth/login"), null, Map.of("email", email, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("JOBPULSE_AUTH");
    }

    private Cookie register(Map<String, String> body) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getCookie("JOBPULSE_AUTH");
    }

    /** A JSON write request with a valid CSRF token, as the SPA would send it. */
    protected MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder builder, Cookie auth, Object body)
            throws Exception {
        builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return auth == null ? builder : builder.cookie(auth);
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** Posts a job as the given employer and returns its id. */
    protected String postJob(Cookie employer, String title, String location, String type, String mode) throws Exception {
        Map<String, Object> job = Map.of(
                "title", title,
                "description", "A real description that is comfortably longer than fifty characters for validation.",
                "location", location,
                "workMode", mode,
                "employmentType", type,
                "experienceLevel", "ENTRY",
                "salaryMin", 600000, "salaryMax", 900000, "salaryCurrency", "INR",
                "skills", java.util.List.of("Java", "Spring Boot"));
        MvcResult result = mvc.perform(withJson(post("/api/employer/jobs"), employer, job))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }
}

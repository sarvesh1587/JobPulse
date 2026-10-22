package com.jobpulse.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobpulse.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileAndSavedJobsIntegrationTest extends IntegrationTest {

    private Map<String, Object> profile(List<String> skills, List<Map<String, Object>> educations) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Neha Joshi");
        body.put("headline", "Backend developer");
        body.put("location", "Pune");
        body.put("experienceYears", 1);
        body.put("summary", "I like building APIs.");
        body.put("preferredRoles", List.of("Backend"));
        body.put("skills", skills);
        body.put("educations", educations);
        body.put("experiences", List.of(Map.of("companyName", "Acme", "title", "Intern", "startDate", "2025-06-01")));
        return body;
    }

    @Test
    void newProfileIsEmptyAndShowsWhatIsMissing() throws Exception {
        Cookie candidate = registerCandidate(uniqueEmail("fresh"));
        mvc.perform(get("/api/profile").cookie(candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills").isEmpty())
                .andExpect(jsonPath("$.completeness.percent").value(0))
                .andExpect(jsonPath("$.completeness.missing.length()").value(8));
    }

    @Test
    void profileUpdatePersistsAndReconcilesChildren() throws Exception {
        Cookie candidate = registerCandidate(uniqueEmail("profile"));
        var edu = Map.<String, Object>of("institution", "COEP", "degree", "B.Tech", "startYear", 2022, "endYear", 2026);

        JsonNode first = body(mvc.perform(withJson(put("/api/profile"), candidate,
                        profile(List.of("Java", "spring boot", "SQL"), List.of(edu))))
                .andExpect(status().isOk()).andReturn());
        assertThat(first.get("skills")).hasSize(3);
        String educationId = first.get("educations").get(0).get("id").asText();

        // Edit the education in place, keep two skills and swap one. Re-adding
        // an existing skill must not hit the unique constraint.
        var editedEdu = new HashMap<String, Object>(edu);
        editedEdu.put("id", educationId);
        editedEdu.put("degree", "B.Tech (Computer Engineering)");
        JsonNode second = body(mvc.perform(withJson(put("/api/profile"), candidate,
                        profile(List.of("Java", "Spring Boot", "Docker"), List.of(editedEdu))))
                .andExpect(status().isOk()).andReturn());

        assertThat(second.get("educations").get(0).get("id").asText()).isEqualTo(educationId);
        assertThat(second.get("educations").get(0).get("degree").asText()).isEqualTo("B.Tech (Computer Engineering)");
        List<String> skills = json.convertValue(second.get("skills"), List.class);
        assertThat(skills).containsExactlyInAnyOrder("Docker", "Java", "Spring Boot");
        assertThat(second.get("completeness").get("percent").asInt()).isEqualTo(95);  // everything but the resume

        // Survives a fresh read
        mvc.perform(get("/api/profile").cookie(candidate)).andExpect(jsonPath("$.experiences[0].companyName").value("Acme"));
    }

    @Test
    void profileValidationRejectsBadDates() throws Exception {
        Cookie candidate = registerCandidate(uniqueEmail("dates"));
        var badEdu = Map.<String, Object>of("institution", "COEP", "degree", "B.Tech", "startYear", 2026, "endYear", 2022);
        mvc.perform(withJson(put("/api/profile"), candidate, profile(List.of("Java"), List.of(badEdu))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void savingIsIdempotentAndSurvivesANewSession() throws Exception {
        String email = uniqueEmail("saved");
        Cookie candidate = registerCandidate(email);
        Cookie employer = registerEmployer(uniqueEmail("saved-emp"), "Saved Co " + uniqueEmail("z"));
        String jobId = postJob(employer, "Data Analyst", "Mumbai", "FULL_TIME", "ONSITE");

        mvc.perform(withJson(put("/api/saved-jobs/" + jobId), candidate, Map.of())).andExpect(status().isNoContent());
        mvc.perform(withJson(put("/api/saved-jobs/" + jobId), candidate, Map.of())).andExpect(status().isNoContent());

        // Sign in again: the bookmark is in the database, not the session.
        Cookie again = login(email);
        mvc.perform(get("/api/saved-jobs").cookie(again))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].saved").value(true));

        mvc.perform(withJson(delete("/api/saved-jobs/" + jobId), again, Map.of())).andExpect(status().isNoContent());
        mvc.perform(get("/api/saved-jobs").cookie(again)).andExpect(jsonPath("$.totalElements").value(0));
    }
}

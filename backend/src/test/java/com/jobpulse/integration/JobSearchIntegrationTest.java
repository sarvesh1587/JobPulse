package com.jobpulse.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobpulse.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Search runs against real PostgreSQL, so the Specification's SQL (LIKE with
 * ESCAPE, IN lists, the NULLS-LAST ordering) is exercised for real. Each test
 * scopes its query to a unique token in job titles, so jobs created by other
 * tests don't affect the counts.
 */
class JobSearchIntegrationTest extends IntegrationTest {

    private String token;
    private Cookie employer;

    @BeforeEach
    void createJobs() throws Exception {
        token = "zq" + UUID.randomUUID().toString().substring(0, 6);
        employer = registerEmployer(uniqueEmail("search-emp"), "Search Co " + token);
        postJob(employer, token + " Java Intern", "Pune", "INTERNSHIP", "ONSITE");
        postJob(employer, token + " Backend Engineer", "Pune", "FULL_TIME", "HYBRID");
        postJob(employer, token + " Frontend Engineer", "Remote (India)", "FULL_TIME", "REMOTE");
    }

    @Test
    void keywordMatchesTitles() throws Exception {
        assertThat(titles(search("q=" + token))).hasSize(3);
    }

    @Test
    void filtersCombineWithAnd() throws Exception {
        assertThat(titles(search("q=" + token + "&type=FULL_TIME"))).hasSize(2);
        assertThat(titles(search("q=" + token + "&type=FULL_TIME&mode=REMOTE")))
                .containsExactly(token + " Frontend Engineer");
        assertThat(titles(search("q=" + token + "&location=pune"))).hasSize(2);
    }

    @Test
    void repeatedParametersMeanOr() throws Exception {
        assertThat(titles(search("q=" + token + "&mode=ONSITE&mode=REMOTE"))).hasSize(2);
    }

    @Test
    void likeWildcardsInTheQueryAreTreatedLiterally() throws Exception {
        assertThat(titles(search("q=" + token + "%25"))).isEmpty();   // %25 is "%"
        assertThat(titles(search("q=" + token + "_"))).isEmpty();
    }

    @Test
    void paginationReportsTotals() throws Exception {
        JsonNode page = search("q=" + token + "&size=2&page=0");
        assertThat(page.get("content")).hasSize(2);
        assertThat(page.get("totalElements").asInt()).isEqualTo(3);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    void closedJobsDisappearFromSearchButStayViewable() throws Exception {
        String id = postJob(employer, token + " Soon Closed", "Pune", "FULL_TIME", "ONSITE");
        mvc.perform(withJson(post("/api/employer/jobs/" + id + "/close"), employer, java.util.Map.of()))
                .andExpect(status().isOk());

        assertThat(titles(search("q=" + token))).doesNotContain(token + " Soon Closed");
        mvc.perform(get("/api/jobs/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void invalidFilterValueIs400() throws Exception {
        mvc.perform(get("/api/jobs?type=ASTRONAUT")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs?size=500")).andExpect(status().isBadRequest());
    }

    @Test
    void savedFlagIsPersonal() throws Exception {
        Cookie candidate = registerCandidate(uniqueEmail("saver"));
        String jobId = search("q=" + token + " Java").get("content").get(0).get("id").asText();
        mvc.perform(withJson(put("/api/saved-jobs/" + jobId), candidate, java.util.Map.of())).andExpect(status().isNoContent());

        JsonNode mine = json.readTree(mvc.perform(get("/api/jobs?q=" + token + " Java").cookie(candidate))
                .andReturn().getResponse().getContentAsString());
        assertThat(mine.get("content").get(0).get("saved").asBoolean()).isTrue();
        assertThat(search("q=" + token + " Java").get("content").get(0).get("saved").asBoolean()).isFalse();
    }

    @Test
    void matchScoreNeedsASignedInCandidate() throws Exception {
        String jobId = search("q=" + token).get("content").get(0).get("id").asText();
        mvc.perform(get("/api/jobs/" + jobId + "/match")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/jobs/" + jobId + "/match").cookie(employer)).andExpect(status().isForbidden());

        Cookie candidate = registerCandidate(uniqueEmail("matcher"));
        mvc.perform(get("/api/jobs/" + jobId + "/match").cookie(candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.missingSkills").isArray());
    }

    private JsonNode search(String query) throws Exception {
        return json.readTree(mvc.perform(get("/api/jobs?" + query)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private List<String> titles(JsonNode page) {
        List<String> titles = new ArrayList<>();
        page.get("content").forEach(j -> titles.add(j.get("title").asText()));
        return titles;
    }
}

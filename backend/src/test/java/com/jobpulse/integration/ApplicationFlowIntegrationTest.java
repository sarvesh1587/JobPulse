package com.jobpulse.integration;

import com.jobpulse.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The full apply flow across candidate and employer, plus the authorization
 * rules: role checks (403) and ownership checks (404, so ids don't leak).
 */
class ApplicationFlowIntegrationTest extends IntegrationTest {

    private Cookie employer;
    private Cookie candidate;
    private String jobId;

    @BeforeEach
    void setUp() throws Exception {
        employer = registerEmployer(uniqueEmail("flow-emp"), "Flow Co " + uniqueEmail("x"));
        candidate = registerCandidate(uniqueEmail("flow-cand"));
        jobId = postJob(employer, "Backend Engineer", "Pune", "FULL_TIME", "HYBRID");
    }

    private String apply(Cookie who) throws Exception {
        var result = mvc.perform(withJson(post("/api/applications"), who,
                        Map.of("jobId", jobId, "coverLetter", "I'd love to help.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.history[0].toStatus").value("APPLIED"))
                .andReturn();
        return body(result).get("id").asText();
    }

    @Test
    void candidateAppliesAndSeesItInTheirTracker() throws Exception {
        String applicationId = apply(candidate);

        mvc.perform(get("/api/applications").cookie(candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(applicationId))
                .andExpect(jsonPath("$[0].job.title").value("Backend Engineer"));

        mvc.perform(get("/api/jobs/" + jobId).cookie(candidate))
                .andExpect(jsonPath("$.myApplication.status").value("APPLIED"));
    }

    @Test
    void applyingTwiceIs409() throws Exception {
        apply(candidate);
        mvc.perform(withJson(post("/api/applications"), candidate, Map.of("jobId", jobId)))
                .andExpect(status().isConflict());
    }

    @Test
    void anotherCandidateCannotSeeMyApplication() throws Exception {
        String applicationId = apply(candidate);
        Cookie stranger = registerCandidate(uniqueEmail("stranger"));

        mvc.perform(get("/api/applications/" + applicationId).cookie(stranger)).andExpect(status().isNotFound());
        mvc.perform(withJson(post("/api/applications/" + applicationId + "/withdraw"), stranger, Map.of()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherEmployerCannotSeeOrMoveTheApplication() throws Exception {
        String applicationId = apply(candidate);
        Cookie otherEmployer = registerEmployer(uniqueEmail("other-emp"), "Other Co " + uniqueEmail("y"));

        mvc.perform(get("/api/employer/applications/" + applicationId).cookie(otherEmployer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/employer/jobs/" + jobId + "/applications").cookie(otherEmployer))
                .andExpect(status().isNotFound());
        mvc.perform(withJson(patch("/api/employer/applications/" + applicationId + "/status"), otherEmployer,
                        Map.of("status", "REJECTED")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rolesAreEnforcedByTheBackend() throws Exception {
        mvc.perform(get("/api/employer/jobs").cookie(candidate)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").cookie(candidate)).andExpect(status().isForbidden());
        mvc.perform(get("/api/applications").cookie(employer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/employer/jobs")).andExpect(status().isUnauthorized());
    }

    @Test
    void employerMovesThePipelineAndCandidateSeesTheInterview() throws Exception {
        String applicationId = apply(candidate);

        mvc.perform(get("/api/employer/jobs/" + jobId + "/applications").cookie(employer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(applicationId))
                .andExpect(jsonPath("$.content[0].coverLetter").value("I'd love to help."))
                .andExpect(jsonPath("$.content[0].notes").doesNotExist());

        Instant interview = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        mvc.perform(withJson(patch("/api/employer/applications/" + applicationId + "/status"), employer,
                        Map.of("status", "INTERVIEW", "interviewAt", interview.toString(), "note", "Panel round")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));

        mvc.perform(get("/api/dashboard").cookie(candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingInterviews[0].id").value(applicationId))
                .andExpect(jsonPath("$.applicationsByStatus.INTERVIEW").value(1));

        mvc.perform(get("/api/applications/" + applicationId).cookie(candidate))
                .andExpect(jsonPath("$.history[1].changedBy").value("EMPLOYER"))
                .andExpect(jsonPath("$.history[1].note").value("Panel round"));
    }

    @Test
    void invalidTransitionsAreRejected() throws Exception {
        String applicationId = apply(candidate);
        mvc.perform(withJson(patch("/api/employer/applications/" + applicationId + "/status"), employer,
                        Map.of("status", "REJECTED"))).andExpect(status().isOk());

        mvc.perform(withJson(patch("/api/employer/applications/" + applicationId + "/status"), employer,
                        Map.of("status", "INTERVIEW")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE"));
    }

    @Test
    void employerCannotWithdrawAndCandidateCannotSetEmployerStatuses() throws Exception {
        String applicationId = apply(candidate);
        mvc.perform(withJson(patch("/api/employer/applications/" + applicationId + "/status"), employer,
                Map.of("status", "WITHDRAWN"))).andExpect(status().isUnprocessableEntity());
        mvc.perform(withJson(patch("/api/applications/" + applicationId), candidate,
                Map.of("status", "OFFER"))).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void withdrawThenReapplyReusesTheSameRecord() throws Exception {
        String applicationId = apply(candidate);
        mvc.perform(withJson(post("/api/applications/" + applicationId + "/withdraw"), candidate, Map.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        mvc.perform(withJson(post("/api/applications"), candidate, Map.of("jobId", jobId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(applicationId))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void cannotApplyToAClosedJob() throws Exception {
        mvc.perform(withJson(post("/api/employer/jobs/" + jobId + "/close"), employer, Map.of())).andExpect(status().isOk());
        mvc.perform(withJson(post("/api/applications"), candidate, Map.of("jobId", jobId)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void notesArePrivateToTheCandidate() throws Exception {
        String applicationId = apply(candidate);
        mvc.perform(withJson(patch("/api/applications/" + applicationId), candidate,
                        Map.of("notes", "Ask about on-call", "nextAction", "Prepare system design")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Ask about on-call"));

        mvc.perform(get("/api/employer/applications/" + applicationId).cookie(employer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.nextAction").doesNotExist());
    }
}

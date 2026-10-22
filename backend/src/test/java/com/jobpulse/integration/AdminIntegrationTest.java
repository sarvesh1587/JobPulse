package com.jobpulse.integration;

import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationTest extends IntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Cookie admin;
    private User adminUser;

    @BeforeEach
    void createAdmin() throws Exception {
        String email = uniqueEmail("admin");
        adminUser = userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), "Admin", Role.ADMIN));
        admin = login(email);
    }

    @Test
    void deactivatedUserLosesAccessImmediately() throws Exception {
        String email = uniqueEmail("victim");
        Cookie candidate = registerCandidate(email);
        mvc.perform(get("/api/dashboard").cookie(candidate)).andExpect(status().isOk());

        String userId = userRepository.findByEmail(email).orElseThrow().getId().toString();
        mvc.perform(withJson(patch("/api/admin/users/" + userId), admin, Map.of("active", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // The same, still-unexpired cookie no longer works...
        mvc.perform(get("/api/dashboard").cookie(candidate)).andExpect(status().isUnauthorized());
        // ...and signing in again is refused.
        mvc.perform(withJson(post("/api/auth/login"), null, Map.of("email", email, "password", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void adminCannotDeactivateThemselves() throws Exception {
        mvc.perform(withJson(patch("/api/admin/users/" + adminUser.getId()), admin, Map.of("active", false)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminCanTakeDownAnyListing() throws Exception {
        Cookie employer = registerEmployer(uniqueEmail("mod-emp"), "Moderated Co " + uniqueEmail("m"));
        String jobId = postJob(employer, "Questionable Listing", "Pune", "FULL_TIME", "ONSITE");

        mvc.perform(withJson(post("/api/admin/jobs/" + jobId + "/close"), admin, Map.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(get("/api/admin/stats").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs").isNumber());
    }
}

package com.jobpulse.integration;

import com.jobpulse.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registerSetsHttpOnlyCookieAndMeReturnsTheUser() throws Exception {
        String email = uniqueEmail("auth");
        var result = mvc.perform(withJson(post("/api/auth/register"), null,
                        Map.of("email", email, "password", PASSWORD, "fullName", "Priya Shah", "role", "CANDIDATE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("JOBPULSE_AUTH=").contains("HttpOnly").contains("SameSite=Lax");

        Cookie auth = result.getResponse().getCookie("JOBPULSE_AUTH");
        mvc.perform(get("/api/auth/me").cookie(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Priya Shah"));
    }

    @Test
    void meWithoutCookieIs401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsRejectedWith409() throws Exception {
        String email = uniqueEmail("dup");
        registerCandidate(email);
        mvc.perform(withJson(post("/api/auth/register"), null,
                        Map.of("email", email.toUpperCase(), "password", PASSWORD, "fullName", "Again", "role", "CANDIDATE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mvc.perform(withJson(post("/api/auth/register"), null,
                        Map.of("email", "not-an-email", "password", "short", "fullName", "", "role", "CANDIDATE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    void nobodyCanSelfRegisterAsAdmin() throws Exception {
        mvc.perform(withJson(post("/api/auth/register"), null,
                        Map.of("email", uniqueEmail("admin"), "password", PASSWORD, "fullName", "Sneaky", "role", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.roleSelfServe").exists());
    }

    @Test
    void employerRegistrationRequiresCompanyName() throws Exception {
        mvc.perform(withJson(post("/api/auth/register"), null,
                        Map.of("email", uniqueEmail("emp"), "password", PASSWORD, "fullName", "Recruiter", "role", "EMPLOYER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.companyNamePresentForEmployer").exists());
    }

    @Test
    void loginWithWrongPasswordIs401AndRightPasswordSetsCookie() throws Exception {
        String email = uniqueEmail("login");
        registerCandidate(email);

        mvc.perform(withJson(post("/api/auth/login"), null, Map.of("email", email, "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));

        mvc.perform(withJson(post("/api/auth/login"), null, Map.of("email", email, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("JOBPULSE_AUTH=")));
    }

    @Test
    void writeRequestWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"whatever1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void tamperedCookieIsTreatedAsSignedOut() throws Exception {
        mvc.perform(get("/api/dashboard").cookie(new Cookie("JOBPULSE_AUTH", "not.a.jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void logoutClearsTheCookie() throws Exception {
        Cookie auth = registerCandidate(uniqueEmail("logout"));
        var result = mvc.perform(withJson(post("/api/auth/logout"), auth, Map.of()))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}

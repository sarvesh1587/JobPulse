package com.jobpulse.controller;

import com.jobpulse.dto.auth.AuthDtos.CurrentUserResponse;
import com.jobpulse.dto.auth.AuthDtos.LoginRequest;
import com.jobpulse.dto.auth.AuthDtos.RegisterRequest;
import com.jobpulse.entity.User;
import com.jobpulse.security.AuthCookieService;
import com.jobpulse.security.JwtService;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthCookieService authCookieService;

    /**
     * Reading the CsrfToken makes Spring write the XSRF-TOKEN cookie. The SPA
     * calls this once on load so it has a token before its first POST.
     */
    @Operation(summary = "Issue the CSRF cookie")
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName());
    }

    @Operation(summary = "Create a candidate or employer account and sign in")
    @PostMapping("/register")
    public ResponseEntity<CurrentUserResponse> register(@Valid @RequestBody RegisterRequest request,
                                                        HttpServletResponse response) {
        User user = authService.register(request);
        signIn(user.getId(), response);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.currentUser(user.getId()));
    }

    @Operation(summary = "Sign in; sets the HttpOnly auth cookie")
    @PostMapping("/login")
    public CurrentUserResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        UUID userId = authService.authenticate(request);
        signIn(userId, response);
        return authService.currentUser(userId);
    }

    @Operation(summary = "Sign out; clears the auth cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookieService.clear(response);
        return ResponseEntity.noContent().build();
    }

    /** 401 when signed out. The SPA uses this on load to restore the session. */
    @Operation(summary = "The signed-in user")
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(authService.currentUser(principal.getId()));
    }

    private void signIn(UUID userId, HttpServletResponse response) {
        authCookieService.writeToken(response, jwtService.issueToken(userId));
    }
}

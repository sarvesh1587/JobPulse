package com.jobpulse.security;

import com.jobpulse.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Turns a valid auth cookie into an authenticated SecurityContext for this
 * request. No session is kept on the server (the API is stateless), so this
 * runs on every request.
 *
 * It isn't a @Component on purpose: Spring Boot registers every Filter bean
 * with the servlet container as well, which would run it twice. SecurityConfig
 * creates it and adds it to the security filter chain only.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthCookieService authCookieService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        authCookieService.readToken(request)
                .flatMap(jwtService::verify)
                .flatMap(userRepository::findById)
                .filter(user -> user.isActive())   // deactivated accounts lose access immediately
                .map(UserPrincipal::new)
                .ifPresent(principal -> SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));

        chain.doFilter(request, response);
    }
}

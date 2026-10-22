package com.jobpulse.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security failures happen in filters, before any controller runs, so
 * @RestControllerAdvice never sees them. This writes the same ApiError JSON
 * for them: 401 when nobody is signed in, 403 when the signed-in user lacks
 * the role, and a distinct code when the CSRF token is missing or stale.
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        write(response, ApiError.of(401, "UNAUTHENTICATED", "Please sign in to continue.", request.getRequestURI()));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        ApiError error = ex instanceof CsrfException
                ? ApiError.of(403, "CSRF_INVALID", "Your security token expired. Refresh the page and try again.", request.getRequestURI())
                : ApiError.of(403, "FORBIDDEN", "You don't have permission to do that.", request.getRequestURI());
        write(response, error);
    }

    private void write(HttpServletResponse response, ApiError error) throws IOException {
        response.setStatus(error.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}

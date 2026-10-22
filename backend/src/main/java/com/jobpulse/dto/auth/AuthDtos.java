package com.jobpulse.dto.auth;

import com.jobpulse.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request and response bodies for /api/auth. */
public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull Role role,
            @Size(max = 200) String companyName
    ) {
        /** Only candidates and employers can sign up. Admins are created by bootstrap. */
        @AssertTrue(message = "must be CANDIDATE or EMPLOYER")
        public boolean isRoleSelfServe() {
            return role == null || role == Role.CANDIDATE || role == Role.EMPLOYER;
        }

        @AssertTrue(message = "is required for employer accounts")
        public boolean isCompanyNamePresentForEmployer() {
            return role != Role.EMPLOYER || (companyName != null && !companyName.isBlank());
        }
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    /** The signed-in user as the SPA needs it. The token itself is never in a body, only in the cookie. */
    public record CurrentUserResponse(
            UUID id,
            String email,
            String fullName,
            Role role,
            UUID companyId
    ) {}
}

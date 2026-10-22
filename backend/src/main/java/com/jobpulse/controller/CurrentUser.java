package com.jobpulse.controller;

import com.jobpulse.entity.Role;
import com.jobpulse.security.UserPrincipal;

import java.util.UUID;

/** Helpers for public endpoints that personalise their response for signed-in candidates. */
final class CurrentUser {

    private CurrentUser() {}

    /** The caller's id if they're a signed-in candidate, otherwise null. */
    static UUID candidateIdOrNull(UserPrincipal principal) {
        return principal != null && principal.getRole() == Role.CANDIDATE ? principal.getId() : null;
    }
}

package com.jobpulse.dto;

import java.time.Instant;

public record ApplicationUpdateRequest(
        String status,        // one of JobApplication.Status — validated in the service
        Instant appliedAt,    // optional override; auto-set when status moves to APPLIED if omitted
        String nextAction,
        String notes
) {}

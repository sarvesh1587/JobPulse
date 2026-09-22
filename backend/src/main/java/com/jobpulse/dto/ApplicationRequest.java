package com.jobpulse.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplicationRequest(
        @NotNull UUID jobId,
        String status,      // optional — defaults to SAVED if omitted
        String nextAction,
        String notes
) {}

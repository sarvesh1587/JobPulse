package com.jobpulse.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UUID userId,
        String email,
        String fullName
) {}

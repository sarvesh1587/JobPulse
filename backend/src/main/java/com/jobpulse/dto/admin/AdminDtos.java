package com.jobpulse.dto.admin;

import com.jobpulse.entity.Job;
import com.jobpulse.entity.Role;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** Admin console bodies (/api/admin). */
public final class AdminDtos {

    private AdminDtos() {}

    public record PlatformStatsResponse(
            long candidates,
            long employers,
            long activeJobs,
            long totalJobs,
            long applications
    ) {}

    public record AdminUserResponse(
            UUID id,
            String email,
            String fullName,
            Role role,
            boolean active,
            Instant createdAt
    ) {}

    public record UserStatusRequest(@NotNull Boolean active) {}

    public record AdminJobResponse(
            UUID id,
            String title,
            String companyName,
            Job.Origin origin,
            Job.JobStatus status,
            Instant postedAt
    ) {}
}

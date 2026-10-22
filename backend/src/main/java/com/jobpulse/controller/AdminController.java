package com.jobpulse.controller;

import com.jobpulse.dto.admin.AdminDtos.AdminJobResponse;
import com.jobpulse.dto.admin.AdminDtos.AdminUserResponse;
import com.jobpulse.dto.admin.AdminDtos.PlatformStatsResponse;
import com.jobpulse.dto.admin.AdminDtos.UserStatusRequest;
import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.entity.Role;
import com.jobpulse.ingestion.IngestionProperties;
import com.jobpulse.ingestion.IngestionResult;
import com.jobpulse.ingestion.IngestionService;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Everything under /api/admin requires the ADMIN role (SecurityConfig). */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final IngestionService ingestionService;
    private final IngestionProperties ingestionProperties;

    @Operation(summary = "Platform-wide counts")
    @GetMapping("/stats")
    public PlatformStatsResponse stats() {
        return adminService.stats();
    }

    @Operation(summary = "Search users")
    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Role role,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return adminService.users(q, role, page, size);
    }

    @Operation(summary = "Activate or deactivate a user")
    @PatchMapping("/users/{id}")
    public AdminUserResponse setActive(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                                       @Valid @RequestBody UserStatusRequest request) {
        return adminService.setActive(principal.getId(), id, request.active());
    }

    @Operation(summary = "Search all job listings")
    @GetMapping("/jobs")
    public PageResponse<AdminJobResponse> jobs(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return adminService.jobs(q, page, size);
    }

    @Operation(summary = "Take a listing down")
    @PostMapping("/jobs/{id}/close")
    public AdminJobResponse closeJob(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return adminService.setJobOpen(principal.getId(), id, false);
    }

    @Operation(summary = "Restore a listing")
    @PostMapping("/jobs/{id}/reopen")
    public AdminJobResponse reopenJob(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return adminService.setJobOpen(principal.getId(), id, true);
    }

    @Operation(summary = "Run ATS ingestion now for the configured targets")
    @PostMapping("/ingestion/run")
    public List<IngestionResult> runIngestion() {
        return ingestionService.runAll(ingestionProperties.targets() == null ? List.of() : ingestionProperties.targets());
    }
}

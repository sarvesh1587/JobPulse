package com.jobpulse.controller;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.SavedJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * PUT and DELETE on /api/saved-jobs/{jobId}: both are idempotent, so the
 * same request repeated gives the same result. That's why these aren't POST.
 */
@Tag(name = "Saved jobs (candidate)")
@RestController
@RequestMapping("/api/saved-jobs")
@RequiredArgsConstructor
public class SavedJobController {

    private final SavedJobService savedJobService;

    @Operation(summary = "My saved jobs, newest first")
    @GetMapping
    public PageResponse<JobSummaryResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return savedJobService.list(principal.getId(), page, size);
    }

    @Operation(summary = "Save a job")
    @PutMapping("/{jobId}")
    public ResponseEntity<Void> save(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID jobId) {
        savedJobService.save(principal.getId(), jobId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove a saved job")
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> unsave(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID jobId) {
        savedJobService.unsave(principal.getId(), jobId);
        return ResponseEntity.noContent().build();
    }
}

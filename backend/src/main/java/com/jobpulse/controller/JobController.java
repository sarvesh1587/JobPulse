package com.jobpulse.controller;

import com.jobpulse.dto.JobDetailResponse;
import com.jobpulse.dto.JobSummaryResponse;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public Page<JobSummaryResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.getId() : null;
        return jobService.search(title, location, pageable, userId);
    }

    @GetMapping("/{id}")
    public JobDetailResponse getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.getId() : null;
        return jobService.getById(id, userId);
    }
}

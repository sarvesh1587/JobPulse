package com.jobpulse.controller;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.job.JobDtos.JobDetailResponse;
import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.dto.job.JobDtos.MatchScoreResponse;
import com.jobpulse.dto.job.JobSearchCriteria;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.JobService;
import com.jobpulse.service.SkillCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Jobs")
@RestController
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final SkillCatalogService skillCatalog;

    @Operation(summary = "Search active jobs (public). Signed-in candidates also get saved flags and cached match scores.")
    @GetMapping("/api/jobs")
    public PageResponse<JobSummaryResponse> search(@Valid @ParameterObject JobSearchCriteria criteria,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return jobService.search(criteria, CurrentUser.candidateIdOrNull(principal));
    }

    @Operation(summary = "The newest active jobs, for the landing page")
    @GetMapping("/api/jobs/featured")
    public List<JobSummaryResponse> featured(@AuthenticationPrincipal UserPrincipal principal) {
        return jobService.featured(CurrentUser.candidateIdOrNull(principal));
    }

    @Operation(summary = "Job detail (public)")
    @GetMapping("/api/jobs/{id}")
    public JobDetailResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return jobService.getDetail(id, CurrentUser.candidateIdOrNull(principal));
    }

    @Operation(summary = "Explainable match score for the signed-in candidate (candidate only)")
    @GetMapping("/api/jobs/{id}/match")
    public MatchScoreResponse match(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return jobService.matchScore(principal.getId(), id);
    }

    @Operation(summary = "Skill-name suggestions for autocomplete")
    @GetMapping("/api/skills")
    public List<String> skills(@RequestParam(defaultValue = "") String q) {
        return skillCatalog.suggest(q);
    }
}

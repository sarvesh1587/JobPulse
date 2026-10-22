package com.jobpulse.controller;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.employer.EmployerDtos.CompanyUpdateRequest;
import com.jobpulse.dto.employer.EmployerDtos.EmployerApplicationResponse;
import com.jobpulse.dto.employer.EmployerDtos.EmployerCompanyResponse;
import com.jobpulse.dto.employer.EmployerDtos.EmployerJobResponse;
import com.jobpulse.dto.employer.EmployerDtos.EmployerOverviewResponse;
import com.jobpulse.dto.employer.EmployerDtos.JobUpsertRequest;
import com.jobpulse.dto.employer.EmployerDtos.StatusUpdateRequest;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.EmployerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/** Everything under /api/employer requires the EMPLOYER role (SecurityConfig). Ownership is checked in EmployerService. */
@Tag(name = "Employer workspace")
@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final EmployerService employerService;

    @Operation(summary = "Job and pipeline counts for my company")
    @GetMapping("/overview")
    public EmployerOverviewResponse overview(@AuthenticationPrincipal UserPrincipal principal) {
        return employerService.overview(principal.getId());
    }

    @Operation(summary = "My company profile")
    @GetMapping("/company")
    public EmployerCompanyResponse company(@AuthenticationPrincipal UserPrincipal principal) {
        return employerService.getCompany(principal.getId());
    }

    @Operation(summary = "Update my company profile")
    @PutMapping("/company")
    public EmployerCompanyResponse updateCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody CompanyUpdateRequest request) {
        return employerService.updateCompany(principal.getId(), request);
    }

    @Operation(summary = "My company's job listings")
    @GetMapping("/jobs")
    public PageResponse<EmployerJobResponse> jobs(@AuthenticationPrincipal UserPrincipal principal,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return employerService.listJobs(principal.getId(), page, size);
    }

    @Operation(summary = "Post a job")
    @PostMapping("/jobs")
    public ResponseEntity<EmployerJobResponse> createJob(@AuthenticationPrincipal UserPrincipal principal,
                                                         @Valid @RequestBody JobUpsertRequest request) {
        EmployerJobResponse created = employerService.createJob(principal.getId(), request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "One of my listings")
    @GetMapping("/jobs/{id}")
    public EmployerJobResponse job(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return employerService.getJob(principal.getId(), id);
    }

    @Operation(summary = "Edit a listing")
    @PutMapping("/jobs/{id}")
    public EmployerJobResponse updateJob(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                                         @Valid @RequestBody JobUpsertRequest request) {
        return employerService.updateJob(principal.getId(), id, request);
    }

    @Operation(summary = "Close a listing to new applications")
    @PostMapping("/jobs/{id}/close")
    public EmployerJobResponse close(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return employerService.setJobOpen(principal.getId(), id, false);
    }

    @Operation(summary = "Reopen a closed listing")
    @PostMapping("/jobs/{id}/reopen")
    public EmployerJobResponse reopen(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return employerService.setJobOpen(principal.getId(), id, true);
    }

    @Operation(summary = "Applicants for one of my listings")
    @GetMapping("/jobs/{id}/applications")
    public PageResponse<EmployerApplicationResponse> applications(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @PathVariable UUID id,
                                                                  @RequestParam(required = false) ApplicationStatus status,
                                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return employerService.listApplications(principal.getId(), id, status, page, size);
    }

    @Operation(summary = "One application to my company, with history")
    @GetMapping("/applications/{id}")
    public EmployerApplicationResponse application(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return employerService.getApplication(principal.getId(), id);
    }

    @Operation(summary = "Move an application through the pipeline")
    @PatchMapping("/applications/{id}/status")
    public EmployerApplicationResponse updateStatus(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                                                    @Valid @RequestBody StatusUpdateRequest request) {
        return employerService.updateApplicationStatus(principal.getId(), id, request);
    }
}

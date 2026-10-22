package com.jobpulse.controller;

import com.jobpulse.dto.application.ApplicationDtos.ApplicationDetailResponse;
import com.jobpulse.dto.application.ApplicationDtos.ApplicationResponse;
import com.jobpulse.dto.application.ApplicationDtos.CreateApplicationRequest;
import com.jobpulse.dto.application.ApplicationDtos.UpdateApplicationRequest;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Tag(name = "Applications (candidate)")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "My applications, optionally filtered by status")
    @GetMapping
    public List<ApplicationResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(required = false) ApplicationStatus status) {
        return applicationService.list(principal.getId(), status);
    }

    @Operation(summary = "Apply in-app, or start tracking an external application")
    @PostMapping
    public ResponseEntity<ApplicationDetailResponse> apply(@AuthenticationPrincipal UserPrincipal principal,
                                                           @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationDetailResponse created = applicationService.apply(principal.getId(), request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "One of my applications, with its status history")
    @GetMapping("/{id}")
    public ApplicationDetailResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return applicationService.get(principal.getId(), id);
    }

    @Operation(summary = "Update notes, or the status of a self-tracked application")
    @PatchMapping("/{id}")
    public ApplicationDetailResponse update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                                            @Valid @RequestBody UpdateApplicationRequest request) {
        return applicationService.update(principal.getId(), id, request);
    }

    @Operation(summary = "Withdraw an application")
    @PostMapping("/{id}/withdraw")
    public ApplicationDetailResponse withdraw(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return applicationService.withdraw(principal.getId(), id);
    }
}

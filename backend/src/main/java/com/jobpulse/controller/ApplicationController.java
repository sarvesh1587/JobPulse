package com.jobpulse.controller;

import com.jobpulse.dto.ApplicationRequest;
import com.jobpulse.dto.ApplicationResponse;
import com.jobpulse.dto.ApplicationUpdateRequest;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public List<ApplicationResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return applicationService.listForUser(principal.getId());
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createOrUpdate(principal.getId(), request));
    }

    @PatchMapping("/{id}")
    public ApplicationResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody ApplicationUpdateRequest request
    ) {
        return applicationService.update(principal.getId(), id, request);
    }
}

package com.jobpulse.controller;

import com.jobpulse.dto.ProfileRequest;
import com.jobpulse.dto.ProfileResponse;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return profileService.getByUserId(principal.getId());
    }

    @PutMapping
    public ProfileResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileRequest request
    ) {
        return profileService.update(principal.getId(), request);
    }
}

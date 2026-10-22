package com.jobpulse.controller;

import com.jobpulse.dto.dashboard.DashboardResponse;
import com.jobpulse.dto.profile.ProfileDtos.ProfileRequest;
import com.jobpulse.dto.profile.ProfileDtos.ProfileResponse;
import com.jobpulse.dto.profile.ResumeAnalysisResponse;
import com.jobpulse.dto.skillgap.SkillGapResponse;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.DashboardService;
import com.jobpulse.service.ProfileService;
import com.jobpulse.service.SkillGapService;
import com.jobpulse.service.resume.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The candidate's own data: profile, resume, dashboard and skill gap. */
@Tag(name = "Candidate profile")
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ResumeService resumeService;
    private final DashboardService dashboardService;
    private final SkillGapService skillGapService;

    @Operation(summary = "My profile, with a completeness checklist")
    @GetMapping("/api/profile")
    public ProfileResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return profileService.get(principal.getId());
    }

    @Operation(summary = "Replace my profile (skills, education and experience included)")
    @PutMapping("/api/profile")
    public ProfileResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                  @Valid @RequestBody ProfileRequest request) {
        return profileService.update(principal.getId(), request);
    }

    /** consentToStore defaults to false: analysing a file never stores it unless the candidate opts in. */
    @Operation(summary = "Analyse a PDF/DOCX resume; stores it only with consentToStore=true")
    @PostMapping(value = "/api/profile/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeAnalysisResponse uploadResume(@AuthenticationPrincipal UserPrincipal principal,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(defaultValue = "false") boolean consentToStore) {
        return resumeService.analyze(principal.getId(), file, consentToStore);
    }

    @Operation(summary = "Candidate dashboard")
    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.build(principal.getId());
    }

    @Operation(summary = "Most-requested skills in my target roles that I don't have yet")
    @GetMapping("/api/skill-gap")
    public SkillGapResponse skillGap(@AuthenticationPrincipal UserPrincipal principal) {
        return skillGapService.compute(principal.getId());
    }
}

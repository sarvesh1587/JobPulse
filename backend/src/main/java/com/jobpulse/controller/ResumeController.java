package com.jobpulse.controller;

import com.jobpulse.dto.ResumeAnalysisResponse;
import com.jobpulse.resume.ResumeService;
import com.jobpulse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * consentToStore defaults to false on purpose — storing a resume is an
     * opt-in action, not the default behavior of "analyze this file".
     */
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResumeAnalysisResponse analyze(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "consentToStore", defaultValue = "false") boolean consentToStore
    ) {
        return resumeService.analyze(principal.getId(), file, consentToStore);
    }
}

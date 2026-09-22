package com.jobpulse.controller;

import com.jobpulse.dto.SkillGapResponse;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.skillgap.SkillGapService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recomputes on every call rather than caching — unlike match scores
 * (which get requested per-job, potentially a lot), this is a single
 * summary view the candidate checks occasionally, so the cost of always
 * being current outweighs the cost of recomputation here.
 */
@RestController
@RequestMapping("/api/skill-gap")
@RequiredArgsConstructor
public class SkillGapController {

    private final SkillGapService skillGapService;

    @GetMapping
    public SkillGapResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return skillGapService.computeAndPersist(principal.getId());
    }
}

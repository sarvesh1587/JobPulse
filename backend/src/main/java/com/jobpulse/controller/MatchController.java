package com.jobpulse.controller;

import com.jobpulse.dto.MatchScoreResponse;
import com.jobpulse.matching.MatchBreakdown;
import com.jobpulse.matching.MatchingService;
import com.jobpulse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class MatchController {

    private static final Duration SCORE_MAX_AGE = Duration.ofHours(6);

    private final MatchingService matchingService;

    @GetMapping("/{id}/match")
    public MatchScoreResponse getMatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MatchBreakdown b = matchingService.getOrCompute(principal.getId(), id, SCORE_MAX_AGE);
        return new MatchScoreResponse(
                id,
                b.overallScore(), b.skillScore(), b.experienceScore(),
                b.educationScore(), b.locationScore(), b.freshnessScore(),
                b.matchedSkills(), b.missingSkills(), b.concerns(), b.positiveSignals(),
                Instant.now()
        );
    }
}

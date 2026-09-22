package com.jobpulse.controller;

import com.jobpulse.ingestion.IngestionProperties;
import com.jobpulse.ingestion.IngestionResult;
import com.jobpulse.ingestion.IngestionService;
import com.jobpulse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Manual ingestion trigger, mainly for local testing and ops. Restricted to
 * ADMIN — there's no admin-seeding flow yet (that's a follow-up), so in
 * practice this only works once a user's role is promoted directly in the
 * database.
 */
@RestController
@RequestMapping("/api/internal/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;
    private final IngestionProperties ingestionProperties;

    @PostMapping("/run")
    public ResponseEntity<List<IngestionResult>> runNow(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || !"ADMIN".equals(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(ingestionService.runAll(ingestionProperties.targets()));
    }
}

package com.jobpulse.controller;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.company.CompanyDtos.CompanyDetailResponse;
import com.jobpulse.dto.company.CompanyDtos.CompanySummaryResponse;
import com.jobpulse.security.UserPrincipal;
import com.jobpulse.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Companies")
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Companies, ordered by number of open roles")
    @GetMapping
    public PageResponse<CompanySummaryResponse> list(@RequestParam(required = false) String q,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "24") @Min(1) @Max(50) int size) {
        return companyService.list(q, page, size);
    }

    @Operation(summary = "A company and its open roles")
    @GetMapping("/{id}")
    public CompanyDetailResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return companyService.get(id, CurrentUser.candidateIdOrNull(principal));
    }
}

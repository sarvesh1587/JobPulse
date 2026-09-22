package com.jobpulse.controller;

import com.jobpulse.company.CompanyService;
import com.jobpulse.dto.CompanyDetailResponse;
import com.jobpulse.dto.CompanySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public Page<CompanySummaryResponse> list(Pageable pageable) {
        return companyService.list(pageable);
    }

    @GetMapping("/{id}")
    public CompanyDetailResponse getById(@PathVariable UUID id) {
        return companyService.getById(id);
    }
}

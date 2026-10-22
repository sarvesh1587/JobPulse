package com.jobpulse.dto.dashboard;

import com.jobpulse.dto.application.ApplicationDtos.ApplicationResponse;
import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.dto.profile.ProfileDtos.Completeness;
import com.jobpulse.entity.ApplicationStatus;

import java.util.List;
import java.util.Map;

/**
 * Everything on the candidate dashboard, computed from that candidate's own
 * rows. A new account gets zeros and empty lists, never placeholder numbers.
 */
public record DashboardResponse(
        String firstName,
        Map<ApplicationStatus, Long> applicationsByStatus,
        long activeApplications,
        long savedJobs,
        List<ApplicationResponse> upcomingInterviews,
        List<ApplicationResponse> recentApplications,
        List<JobSummaryResponse> recentlySaved,
        RecommendationBasis recommendationBasis,
        List<JobSummaryResponse> recommended,
        Completeness profileCompleteness
) {
    /** SKILLS when recommendations come from skill overlap, RECENT when the profile has no skills yet. */
    public enum RecommendationBasis { SKILLS, RECENT }
}

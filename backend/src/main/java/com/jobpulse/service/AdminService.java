package com.jobpulse.service;

import com.jobpulse.dto.admin.AdminDtos.AdminJobResponse;
import com.jobpulse.dto.admin.AdminDtos.AdminUserResponse;
import com.jobpulse.dto.admin.AdminDtos.PlatformStatsResponse;
import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import com.jobpulse.exception.BusinessRuleException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/** Restricted operations. Only reachable with the ADMIN role (see SecurityConfig). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public PlatformStatsResponse stats() {
        return new PlatformStatsResponse(
                userRepository.countByRole(Role.CANDIDATE),
                userRepository.countByRole(Role.EMPLOYER),
                jobRepository.countByStatus(Job.JobStatus.ACTIVE),
                jobRepository.count(),
                applicationRepository.count());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(String query, Role role, int page, int size) {
        var users = userRepository.search(role, likePattern(query),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(users, this::toUserResponse);
    }

    /**
     * Deactivating takes effect on the user's next request, because the auth
     * filter re-checks the active flag every time. An admin can't deactivate
     * their own account, so the platform can't be left with no admin by accident.
     */
    @Transactional
    public AdminUserResponse setActive(UUID adminId, UUID userId, boolean active) {
        if (adminId.equals(userId)) {
            throw new BusinessRuleException("You can't deactivate your own account.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setActive(active);
        log.info("Admin {} set user {} active={}", adminId, userId, active);
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminJobResponse> jobs(String query, int page, int size) {
        var jobs = jobRepository.searchForAdmin(likePattern(query),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(jobs, this::toJobResponse);
    }

    /** Moderation: an admin can take any listing down, ingested or employer-posted. */
    @Transactional
    public AdminJobResponse setJobOpen(UUID adminId, UUID jobId, boolean open) {
        Job job = jobRepository.findWithCompanyById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found."));
        job.setStatus(open ? Job.JobStatus.ACTIVE : Job.JobStatus.CLOSED);
        log.info("Admin {} {} job {}", adminId, open ? "reopened" : "closed", jobId);
        return toJobResponse(job);
    }

    private String likePattern(String query) {
        return query == null || query.isBlank() ? "%" : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private AdminUserResponse toUserResponse(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isActive(), u.getCreatedAt());
    }

    private AdminJobResponse toJobResponse(Job j) {
        return new AdminJobResponse(j.getId(), j.getTitle(), j.getCompany().getName(), j.getOrigin(), j.getStatus(), j.getPostedAt());
    }
}

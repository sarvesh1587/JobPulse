package com.jobpulse.service;

import com.jobpulse.dto.auth.AuthDtos.CurrentUserResponse;
import com.jobpulse.dto.auth.AuthDtos.LoginRequest;
import com.jobpulse.dto.auth.AuthDtos.RegisterRequest;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.Company;
import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import com.jobpulse.exception.DuplicateResourceException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.CompanyRepository;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Creates the account and what the role needs, in one transaction: a
     * candidate gets an empty profile, an employer gets their company. If any
     * step fails, nothing is saved.
     */
    @Transactional
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()),
                request.fullName().trim(), request.role()));

        if (request.role() == Role.EMPLOYER) {
            createCompanyFor(user, request.companyName().trim());
        } else {
            candidateProfileRepository.save(new CandidateProfile(user));
        }
        log.info("Registered new {} account {}", user.getRole(), user.getId());
        return user;
    }

    /**
     * Checks the password through Spring Security's AuthenticationManager,
     * which loads the user via AppUserDetailsService and compares BCrypt
     * hashes. A wrong password throws BadCredentialsException (401) and a
     * deactivated account throws DisabledException (403).
     */
    public UUID authenticate(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
        return ((UserPrincipal) auth.getPrincipal()).getId();
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        UUID companyId = user.getRole() == Role.EMPLOYER
                ? companyRepository.findByOwnerId(userId).map(Company::getId).orElse(null)
                : null;
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), companyId);
    }

    private void createCompanyFor(User owner, String companyName) {
        String normalized = TextNormalizer.normalize(companyName);
        if (companyRepository.existsByNormalizedName(normalized)) {
            throw new DuplicateResourceException(
                    "A company called \"" + companyName + "\" is already on JobPulse. Use a more specific name.");
        }
        Company company = new Company(companyName, normalized);
        company.setOwner(owner);
        companyRepository.save(company);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

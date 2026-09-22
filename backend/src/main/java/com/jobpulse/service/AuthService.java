package com.jobpulse.service;

import com.jobpulse.dto.AuthResponse;
import com.jobpulse.dto.LoginRequest;
import com.jobpulse.dto.RegisterRequest;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.User;
import com.jobpulse.exception.DuplicateResourceException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role("CANDIDATE")
                .active(true)
                .build();
        user = userRepository.save(user);

        CandidateProfile profile = CandidateProfile.builder()
                .user(user)
                .build();
        candidateProfileRepository.save(profile);

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }
}

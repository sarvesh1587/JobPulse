package com.jobpulse.service;

import com.jobpulse.dto.RegisterRequest;
import com.jobpulse.entity.User;
import com.jobpulse.exception.DuplicateResourceException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, candidateProfileRepository, passwordEncoder,
                authenticationManager, jwtService);
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123", "Sarvesh");
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void register_createsUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "Sarvesh");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("new@example.com")
                .passwordHash("hashed")
                .fullName("Sarvesh")
                .role("CANDIDATE")
                .active(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("mock-token");

        var response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("mock-token");
        assertThat(response.email()).isEqualTo("new@example.com");
    }
}

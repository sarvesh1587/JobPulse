package com.jobpulse.config;

import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import com.jobpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Admins can't sign themselves up. On startup, if ADMIN_EMAIL and
 * ADMIN_PASSWORD are set and the database has no admin yet, this creates
 * one. It does nothing on later startups, and nothing if the variables aren't set.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final BootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.adminEmail();
        String password = properties.adminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;
        if (userRepository.existsByRole(Role.ADMIN)) return;

        if (password.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least " + MIN_ADMIN_PASSWORD_LENGTH + " characters.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalized)) {
            throw new IllegalStateException("ADMIN_EMAIL belongs to an existing non-admin account. Use a different email.");
        }
        userRepository.save(new User(normalized, passwordEncoder.encode(password), "Administrator", Role.ADMIN));
        log.info("Created the initial admin account from ADMIN_EMAIL");
    }
}

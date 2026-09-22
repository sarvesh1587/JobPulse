package com.jobpulse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JobpulseApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the whole application context (security, JPA, Flyway-free
        // test profile) wires up without errors.
    }
}

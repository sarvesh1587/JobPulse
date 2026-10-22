package com.jobpulse.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobSpecificationsTest {

    @Test
    void likePatternLowercasesTrimsAndWraps() {
        assertThat(JobSpecifications.likePattern("  Java ")).isEqualTo("%java%");
    }

    @Test
    void likeWildcardsAreEscaped() {
        assertThat(JobSpecifications.likePattern("100%")).isEqualTo("%100\\%%");
        assertThat(JobSpecifications.likePattern("a_b")).isEqualTo("%a\\_b%");
        assertThat(JobSpecifications.likePattern("c:\\")).isEqualTo("%c:\\\\%");
    }
}

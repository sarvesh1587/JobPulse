package com.jobpulse.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"REJECTED", "WITHDRAWN"})
    void terminalStatusesAllowNoTransitions(ApplicationStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
        assertThat(terminal.allowedNext()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"REJECTED", "WITHDRAWN"}, mode = EnumSource.Mode.EXCLUDE)
    void everyOpenStatusCanBeRejectedOrWithdrawn(ApplicationStatus open) {
        assertThat(open.canMoveTo(ApplicationStatus.REJECTED)).isTrue();
        assertThat(open.canMoveTo(ApplicationStatus.WITHDRAWN)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void nothingMovesBackToApplied(ApplicationStatus any) {
        assertThat(any.canMoveTo(ApplicationStatus.APPLIED)).isFalse();
    }

    @Test
    void pipelineOnlyMovesForward() {
        assertThat(ApplicationStatus.INTERVIEW.canMoveTo(ApplicationStatus.IN_REVIEW)).isFalse();
        assertThat(ApplicationStatus.OFFER.canMoveTo(ApplicationStatus.INTERVIEW)).isFalse();
        assertThat(ApplicationStatus.APPLIED.canMoveTo(ApplicationStatus.OFFER)).isTrue();
    }
}

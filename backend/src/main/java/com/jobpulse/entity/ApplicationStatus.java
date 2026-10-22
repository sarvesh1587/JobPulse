package com.jobpulse.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * The application pipeline as a small state machine. Keeping the allowed
 * transitions in one place, rather than spread across services, makes the
 * rules easy to test and explain. REJECTED and WITHDRAWN are terminal.
 * Nothing moves back to APPLIED except a candidate re-applying after
 * withdrawing, which ApplicationService handles explicitly.
 */
public enum ApplicationStatus {
    APPLIED, IN_REVIEW, ASSESSMENT, INTERVIEW, OFFER, REJECTED, WITHDRAWN;

    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case APPLIED -> EnumSet.of(IN_REVIEW, ASSESSMENT, INTERVIEW, OFFER, REJECTED, WITHDRAWN);
            case IN_REVIEW -> EnumSet.of(ASSESSMENT, INTERVIEW, OFFER, REJECTED, WITHDRAWN);
            case ASSESSMENT -> EnumSet.of(INTERVIEW, OFFER, REJECTED, WITHDRAWN);
            case INTERVIEW -> EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER -> EnumSet.of(REJECTED, WITHDRAWN);
            case REJECTED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return this == REJECTED || this == WITHDRAWN;
    }

    /** Statuses that count as "in progress" on dashboards. */
    public static Set<ApplicationStatus> inProgress() {
        return EnumSet.of(APPLIED, IN_REVIEW, ASSESSMENT, INTERVIEW, OFFER);
    }
}

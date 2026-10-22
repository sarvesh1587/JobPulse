package com.jobpulse.service;

import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.entity.User;
import com.jobpulse.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * The one place application status changes are validated and applied, used
 * by both the candidate and the employer services, so the rules can't drift
 * apart. Who may change what is decided by the callers. This class enforces
 * what any change must respect.
 */
@Component
public class ApplicationWorkflow {

    /**
     * Applies a status change and/or interview time.
     *  - Moving to the current status is allowed only to reschedule an interview.
     *  - Otherwise the move must be allowed by ApplicationStatus.allowedNext().
     *  - interviewAt only makes sense while the status is INTERVIEW. Leaving
     *    INTERVIEW clears it, so dashboards never show stale interviews.
     */
    public void transition(JobApplication application, ApplicationStatus next, Instant interviewAt,
                           User actor, String note) {
        ApplicationStatus current = application.getStatus();
        if (next != current) {
            if (!current.canMoveTo(next)) {
                throw new BusinessRuleException(current.isTerminal()
                        ? "This application is " + label(current) + " and can't be changed."
                        : "An application can't move from " + label(current) + " to " + label(next) + ".");
            }
            application.changeStatus(next, actor, note);
        }

        if (next == ApplicationStatus.INTERVIEW) {
            if (interviewAt != null) application.setInterviewAt(interviewAt);
        } else {
            if (interviewAt != null) {
                throw new BusinessRuleException("An interview time can only be set when the status is Interview.");
            }
            application.setInterviewAt(null);
        }
    }

    public List<ApplicationStatus> allowedNext(JobApplication application) {
        return List.copyOf(application.getStatus().allowedNext());
    }

    static String label(ApplicationStatus status) {
        return status.name().replace('_', ' ').toLowerCase();
    }
}

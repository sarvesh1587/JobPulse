package com.jobpulse.service;

import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import com.jobpulse.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationWorkflowTest {

    private final ApplicationWorkflow workflow = new ApplicationWorkflow();
    private final User employer = new User("e@x.com", "hash", "Employer", Role.EMPLOYER);
    private JobApplication application;

    @BeforeEach
    void setUp() {
        application = new JobApplication(new User("c@x.com", "hash", "Candidate", Role.CANDIDATE), new Job(), Instant.now());
    }

    @Test
    void validTransitionChangesStatusAndRecordsHistory() {
        workflow.transition(application, ApplicationStatus.IN_REVIEW, null, employer, "Looks promising");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);
        assertThat(application.getHistory()).singleElement().satisfies(change -> {
            assertThat(change.getFromStatus()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(change.getToStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);
            assertThat(change.getNote()).isEqualTo("Looks promising");
        });
    }

    @Test
    void terminalStatusCannotChange() {
        workflow.transition(application, ApplicationStatus.REJECTED, null, employer, null);

        assertThatThrownBy(() -> workflow.transition(application, ApplicationStatus.INTERVIEW, null, employer, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rejected");
    }

    @Test
    void interviewTimeCanBeRescheduledWithoutAStatusChange() {
        Instant first = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant second = first.plus(1, ChronoUnit.DAYS);
        workflow.transition(application, ApplicationStatus.INTERVIEW, first, employer, null);
        workflow.transition(application, ApplicationStatus.INTERVIEW, second, employer, null);

        assertThat(application.getInterviewAt()).isEqualTo(second);
        assertThat(application.getHistory()).hasSize(1);   // rescheduling isn't a new status change
    }

    @Test
    void leavingInterviewClearsTheInterviewTime() {
        workflow.transition(application, ApplicationStatus.INTERVIEW, Instant.now().plus(1, ChronoUnit.DAYS), employer, null);
        workflow.transition(application, ApplicationStatus.OFFER, null, employer, null);

        assertThat(application.getInterviewAt()).isNull();
    }

    @Test
    void interviewTimeOnlyAllowedWithInterviewStatus() {
        assertThatThrownBy(() -> workflow.transition(application, ApplicationStatus.IN_REVIEW,
                Instant.now().plus(1, ChronoUnit.DAYS), employer, null))
                .isInstanceOf(BusinessRuleException.class);
    }
}

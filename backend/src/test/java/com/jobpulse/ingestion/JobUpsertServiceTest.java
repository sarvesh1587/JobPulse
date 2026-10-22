package com.jobpulse.ingestion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobUpsertServiceTest {

    @Test
    void skillMatchingIsWholeWordAndCaseInsensitive() {
        assertThat(JobUpsertService.containsWord("We use Java and Spring", "java")).isTrue();
        assertThat(JobUpsertService.containsWord("Experience with JavaScript", "Java")).isFalse();
    }

    @Test
    void skillNamesWithSymbolsStillMatch() {
        assertThat(JobUpsertService.containsWord("Strong C++ skills", "C++")).isTrue();
        assertThat(JobUpsertService.containsWord("Node.js backend", "Node.js")).isTrue();
        assertThat(JobUpsertService.containsWord("Build CI/CD pipelines", "CI/CD")).isTrue();
    }
}

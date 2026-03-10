package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.*;

import com.keplerops.groundcontrol.domain.requirements.model.GitHubIssueSync;
import com.keplerops.groundcontrol.domain.requirements.state.IssueState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S2187") // Tests are in @Nested inner classes
class GitHubIssueSyncTest {

    private static GitHubIssueSync createSync() {
        return new GitHubIssueSync(
                42, "Fix bug", IssueState.OPEN, "https://github.com/org/repo/issues/42", Instant.now());
    }

    @Nested
    class Defaults {

        @Test
        void issueLabelsDefaultsToEmptyList() {
            var sync = createSync();
            assertThat(sync.getIssueLabels()).isEmpty();
        }

        @Test
        void issueBodyDefaultsToEmpty() {
            var sync = createSync();
            assertThat(sync.getIssueBody()).isEmpty();
        }

        @Test
        void phaseDefaultsToNull() {
            var sync = createSync();
            assertThat(sync.getPhase()).isNull();
        }

        @Test
        void priorityLabelDefaultsToEmpty() {
            var sync = createSync();
            assertThat(sync.getPriorityLabel()).isEmpty();
        }

        @Test
        void crossReferencesDefaultsToEmptyList() {
            var sync = createSync();
            assertThat(sync.getCrossReferences()).isEmpty();
        }
    }

    @Nested
    class Construction {

        @Test
        void requiredFieldsSetCorrectly() {
            var now = Instant.now();
            var sync = new GitHubIssueSync(
                    99, "Feature request", IssueState.CLOSED, "https://github.com/org/repo/issues/99", now);

            assertThat(sync.getIssueNumber()).isEqualTo(99);
            assertThat(sync.getIssueTitle()).isEqualTo("Feature request");
            assertThat(sync.getIssueState()).isEqualTo(IssueState.CLOSED);
            assertThat(sync.getIssueUrl()).isEqualTo("https://github.com/org/repo/issues/99");
            assertThat(sync.getLastFetchedAt()).isEqualTo(now);
        }
    }

    @Nested
    class Accessors {

        @Test
        void issueTitleSetterWorks() {
            var sync = createSync();
            sync.setIssueTitle("Updated title");
            assertThat(sync.getIssueTitle()).isEqualTo("Updated title");
        }

        @Test
        void issueStateSetterWorks() {
            var sync = createSync();
            sync.setIssueState(IssueState.CLOSED);
            assertThat(sync.getIssueState()).isEqualTo(IssueState.CLOSED);
        }

        @Test
        void issueBodySetterWorks() {
            var sync = createSync();
            sync.setIssueBody("Some body text");
            assertThat(sync.getIssueBody()).isEqualTo("Some body text");
        }

        @Test
        void phaseSetterWorks() {
            var sync = createSync();
            sync.setPhase(1);
            assertThat(sync.getPhase()).isEqualTo(1);
        }

        @Test
        void priorityLabelSetterWorks() {
            var sync = createSync();
            sync.setPriorityLabel("P0");
            assertThat(sync.getPriorityLabel()).isEqualTo("P0");
        }

        @Test
        void issueLabelsSetterWorks() {
            var sync = createSync();
            sync.setIssueLabels(List.of("bug", "priority:high"));
            assertThat(sync.getIssueLabels()).containsExactly("bug", "priority:high");
        }

        @Test
        void crossReferencesSetterWorks() {
            var sync = createSync();
            sync.setCrossReferences(List.of(10, 20, 30));
            assertThat(sync.getCrossReferences()).containsExactly(10, 20, 30);
        }

        @Test
        void lastFetchedAtSetterWorks() {
            var sync = createSync();
            var now = Instant.now();
            sync.setLastFetchedAt(now);
            assertThat(sync.getLastFetchedAt()).isEqualTo(now);
        }

        @Test
        void idIsNullBeforePersist() {
            var sync = createSync();
            assertThat(sync.getId()).isNull();
        }

        @Test
        void timestampsAreNullBeforePersist() {
            var sync = createSync();
            assertThat(sync.getCreatedAt()).isNull();
            assertThat(sync.getUpdatedAt()).isNull();
        }
    }
}

package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.*;

import com.keplerops.groundcontrol.domain.requirements.model.GitHubPullRequestSync;
import com.keplerops.groundcontrol.domain.requirements.state.PullRequestState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S2187") // Tests are in @Nested inner classes
class GitHubPullRequestSyncTest {

    private static GitHubPullRequestSync createSync() {
        return new GitHubPullRequestSync(
                42, "Add feature", PullRequestState.OPEN, "https://github.com/org/repo/pull/42", Instant.now());
    }

    @Nested
    class Defaults {

        @Test
        void prLabelsDefaultsToEmptyList() {
            var sync = createSync();
            assertThat(sync.getPrLabels()).isEmpty();
        }

        @Test
        void prBodyDefaultsToEmpty() {
            var sync = createSync();
            assertThat(sync.getPrBody()).isEmpty();
        }

        @Test
        void baseBranchDefaultsToEmpty() {
            var sync = createSync();
            assertThat(sync.getBaseBranch()).isEmpty();
        }

        @Test
        void headBranchDefaultsToEmpty() {
            var sync = createSync();
            assertThat(sync.getHeadBranch()).isEmpty();
        }
    }

    @Nested
    class Construction {

        @Test
        void requiredFieldsSetCorrectly() {
            var now = Instant.now();
            var sync = new GitHubPullRequestSync(
                    99, "Fix bug", PullRequestState.MERGED, "https://github.com/org/repo/pull/99", now);

            assertThat(sync.getPrNumber()).isEqualTo(99);
            assertThat(sync.getPrTitle()).isEqualTo("Fix bug");
            assertThat(sync.getPrState()).isEqualTo(PullRequestState.MERGED);
            assertThat(sync.getPrUrl()).isEqualTo("https://github.com/org/repo/pull/99");
            assertThat(sync.getLastFetchedAt()).isEqualTo(now);
        }
    }

    @Nested
    class Accessors {

        @Test
        void prTitleSetterWorks() {
            var sync = createSync();
            sync.setPrTitle("Updated title");
            assertThat(sync.getPrTitle()).isEqualTo("Updated title");
        }

        @Test
        void prStateSetterWorks() {
            var sync = createSync();
            sync.setPrState(PullRequestState.CLOSED);
            assertThat(sync.getPrState()).isEqualTo(PullRequestState.CLOSED);
        }

        @Test
        void prBodySetterWorks() {
            var sync = createSync();
            sync.setPrBody("Some body text");
            assertThat(sync.getPrBody()).isEqualTo("Some body text");
        }

        @Test
        void baseBranchSetterWorks() {
            var sync = createSync();
            sync.setBaseBranch("main");
            assertThat(sync.getBaseBranch()).isEqualTo("main");
        }

        @Test
        void headBranchSetterWorks() {
            var sync = createSync();
            sync.setHeadBranch("feature/foo");
            assertThat(sync.getHeadBranch()).isEqualTo("feature/foo");
        }

        @Test
        void prLabelsSetterWorks() {
            var sync = createSync();
            sync.setPrLabels(List.of("enhancement", "ready-for-review"));
            assertThat(sync.getPrLabels()).containsExactly("enhancement", "ready-for-review");
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

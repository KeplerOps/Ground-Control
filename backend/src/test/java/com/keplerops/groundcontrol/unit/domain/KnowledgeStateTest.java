package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KnowledgeStateTest {

    @Nested
    class Values {

        @Test
        void exposesExactlyThreeStates() {
            // The L0 vocabulary is intentionally narrow: a new state would
            // need an explicit requirement update and a coordinated change
            // across asset / relation surfaces, MCP, frontend, and graph
            // projection. Pin the size so a casual addition fails here.
            assertThat(KnowledgeState.values())
                    .containsExactly(KnowledgeState.UNKNOWN, KnowledgeState.PROVISIONAL, KnowledgeState.CONFIRMED);
        }
    }

    @Nested
    class AtLeast {

        @Test
        void confirmedAtLeastEverything() {
            assertThat(KnowledgeState.CONFIRMED.atLeast(KnowledgeState.CONFIRMED))
                    .isTrue();
            assertThat(KnowledgeState.CONFIRMED.atLeast(KnowledgeState.PROVISIONAL))
                    .isTrue();
            assertThat(KnowledgeState.CONFIRMED.atLeast(KnowledgeState.UNKNOWN)).isTrue();
        }

        @Test
        void provisionalAtLeastUnknownAndItself() {
            assertThat(KnowledgeState.PROVISIONAL.atLeast(KnowledgeState.UNKNOWN))
                    .isTrue();
            assertThat(KnowledgeState.PROVISIONAL.atLeast(KnowledgeState.PROVISIONAL))
                    .isTrue();
            assertThat(KnowledgeState.PROVISIONAL.atLeast(KnowledgeState.CONFIRMED))
                    .isFalse();
        }

        @Test
        void unknownOnlyAtLeastItself() {
            assertThat(KnowledgeState.UNKNOWN.atLeast(KnowledgeState.UNKNOWN)).isTrue();
            assertThat(KnowledgeState.UNKNOWN.atLeast(KnowledgeState.PROVISIONAL))
                    .isFalse();
            assertThat(KnowledgeState.UNKNOWN.atLeast(KnowledgeState.CONFIRMED)).isFalse();
        }
    }
}

package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicyRule;
import com.keplerops.groundcontrol.domain.packregistry.repository.TrustPolicyRepository;
import com.keplerops.groundcontrol.domain.packregistry.service.CreateTrustPolicyCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.TrustPolicyService;
import com.keplerops.groundcontrol.domain.packregistry.service.UpdateTrustPolicyCommand;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyField;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyRuleOperator;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrustPolicyServiceTest {

    @Mock
    private TrustPolicyRepository trustPolicyRepository;

    @Mock
    private ProjectService projectService;

    private TrustPolicyService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Project makeProject() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    private TrustPolicyRule rule(
            TrustPolicyField field, TrustPolicyRuleOperator operator, String value, TrustOutcome outcome) {
        return new TrustPolicyRule(field, operator, value, outcome);
    }

    @BeforeEach
    void setUp() {
        service = new TrustPolicyService(trustPolicyRepository, projectService);
    }

    @Nested
    class Create {

        @Test
        void createsNewPolicy() {
            var project = makeProject();
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(trustPolicyRepository.existsByProjectIdAndName(PROJECT_ID, "allow-nist"))
                    .thenReturn(false);
            when(trustPolicyRepository.save(any(TrustPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateTrustPolicyCommand(
                    PROJECT_ID,
                    "allow-nist",
                    "Allow NIST packs",
                    TrustOutcome.REJECTED,
                    List.of(rule(
                            TrustPolicyField.PUBLISHER, TrustPolicyRuleOperator.EQUALS, "NIST", TrustOutcome.TRUSTED)),
                    1,
                    true);

            var result = service.create(command);
            assertThat(result.getName()).isEqualTo("allow-nist");
            assertThat(result.getDefaultOutcome()).isEqualTo(TrustOutcome.REJECTED);
            verify(trustPolicyRepository).save(any(TrustPolicy.class));
        }

        @Test
        void rejectsDuplicateName() {
            var project = makeProject();
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(trustPolicyRepository.existsByProjectIdAndName(PROJECT_ID, "allow-nist"))
                    .thenReturn(true);

            var command =
                    new CreateTrustPolicyCommand(PROJECT_ID, "allow-nist", null, TrustOutcome.REJECTED, null, 1, true);

            assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesExistingPolicy() {
            var project = makeProject();
            var policy = new TrustPolicy(project, "allow-nist", TrustOutcome.REJECTED);
            var policyId = UUID.randomUUID();
            setField(policy, "id", policyId);
            when(trustPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
            when(trustPolicyRepository.save(any(TrustPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

            var command =
                    new UpdateTrustPolicyCommand(null, "Updated description", TrustOutcome.TRUSTED, null, 2, false);

            var result = service.update(policyId, command);
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getDefaultOutcome()).isEqualTo(TrustOutcome.TRUSTED);
            assertThat(result.getPriority()).isEqualTo(2);
            assertThat(result.isEnabled()).isFalse();
        }
    }

    @Nested
    class Get {

        @Test
        void throwsNotFoundForMissingPolicy() {
            var missingId = UUID.randomUUID();
            when(trustPolicyRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(missingId)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesExistingPolicy() {
            var policyId = UUID.randomUUID();
            when(trustPolicyRepository.existsById(policyId)).thenReturn(true);

            service.delete(policyId);
            verify(trustPolicyRepository).deleteById(policyId);
        }

        @Test
        void throwsNotFoundForMissingPolicy() {
            var missingId = UUID.randomUUID();
            when(trustPolicyRepository.existsById(missingId)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(missingId)).isInstanceOf(NotFoundException.class);
        }
    }
}

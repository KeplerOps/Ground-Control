package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.AssetExternalId;
import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetExternalIdRepository;
import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.AssetRelationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.service.AssetService;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetCommand;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetExternalIdCommand;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetLinkCommand;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetRelationCommand;
import com.keplerops.groundcontrol.domain.assets.service.UpdateAssetCommand;
import com.keplerops.groundcontrol.domain.assets.service.UpdateAssetExternalIdCommand;
import com.keplerops.groundcontrol.domain.assets.service.UpdateAssetRelationCommand;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private OperationalAssetRepository assetRepository;

    @Mock
    private AssetRelationRepository relationRepository;

    @Mock
    private AssetLinkRepository linkRepository;

    @Mock
    private AssetExternalIdRepository externalIdRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GraphTargetResolverService graphTargetResolverService;

    @InjectMocks
    private AssetService assetService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private OperationalAsset createAsset(String uid, String name) {
        var asset = new OperationalAsset(project, uid, name);
        setField(asset, "id", UUID.randomUUID());
        return asset;
    }

    @Nested
    class Create {

        @Test
        void createSucceeds() {
            var command =
                    new CreateAssetCommand(projectId, "ASSET-001", "Web Server", "A web server", AssetType.SERVICE);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(assetRepository.existsByProjectIdAndUidIgnoreCase(projectId, "ASSET-001"))
                    .thenReturn(false);
            when(assetRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, OperationalAsset.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var result = assetService.create(command);

            assertThat(result.getUid()).isEqualTo("ASSET-001");
            assertThat(result.getName()).isEqualTo("Web Server");
            assertThat(result.getAssetType()).isEqualTo(AssetType.SERVICE);
        }

        @Test
        void createNormalizesUidToUpperCase() {
            var command = new CreateAssetCommand(projectId, "asset-001", "Web Server", null, null);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(assetRepository.existsByProjectIdAndUidIgnoreCase(projectId, "ASSET-001"))
                    .thenReturn(false);
            when(assetRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, OperationalAsset.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var result = assetService.create(command);

            assertThat(result.getUid()).isEqualTo("ASSET-001");
        }

        @Test
        void createDuplicateUidThrowsConflict() {
            var command = new CreateAssetCommand(projectId, "ASSET-001", "Web Server", null, null);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(assetRepository.existsByProjectIdAndUidIgnoreCase(projectId, "ASSET-001"))
                    .thenReturn(true);

            assertThatThrownBy(() -> assetService.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updateNameOnly() {
            var asset = createAsset("ASSET-001", "Old Name");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateAssetCommand("New Name", null, null);
            var result = assetService.update(asset.getId(), command);

            assertThat(result.getName()).isEqualTo("New Name");
        }

        @Test
        void updateBlankNameThrows() {
            var asset = createAsset("ASSET-001", "Old Name");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            var command = new UpdateAssetCommand("", null, null);

            assertThatThrownBy(() -> assetService.update(asset.getId(), command))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        void updateAllFields() {
            var asset = createAsset("ASSET-001", "Old");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateAssetCommand("New", "A new description", AssetType.DATABASE);
            var result = assetService.update(asset.getId(), command);

            assertThat(result.getName()).isEqualTo("New");
            assertThat(result.getDescription()).isEqualTo("A new description");
            assertThat(result.getAssetType()).isEqualTo(AssetType.DATABASE);
        }
    }

    @Nested
    class Read {

        @Test
        void getByIdReturnsAsset() {
            var asset = createAsset("ASSET-001", "Test");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            var result = assetService.getById(asset.getId());
            assertThat(result.getUid()).isEqualTo("ASSET-001");
        }

        @Test
        void getByIdNotFoundThrows() {
            var id = UUID.randomUUID();
            when(assetRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.getById(id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void getByUidReturnsAsset() {
            var asset = createAsset("ASSET-001", "Test");
            when(assetRepository.findByProjectIdAndUidIgnoreCase(projectId, "ASSET-001"))
                    .thenReturn(Optional.of(asset));

            var result = assetService.getByUid(projectId, "ASSET-001");
            assertThat(result.getUid()).isEqualTo("ASSET-001");
        }

        @Test
        void listByProjectReturnsList() {
            var a1 = createAsset("ASSET-001", "First");
            var a2 = createAsset("ASSET-002", "Second");
            when(assetRepository.findByProjectIdAndArchivedAtIsNull(projectId)).thenReturn(List.of(a1, a2));

            var result = assetService.listByProject(projectId);
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class Archive {

        @Test
        void archiveSetsArchivedAt() {
            var asset = createAsset("ASSET-001", "Test");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = assetService.archive(asset.getId());
            assertThat(result.getArchivedAt()).isNotNull();
        }
    }

    @Nested
    class Delete {

        @Test
        void deleteSucceeds() {
            var asset = createAsset("ASSET-001", "Test");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            assetService.delete(asset.getId());
            verify(assetRepository).delete(asset);
        }
    }

    @Nested
    class Relations {

        @Test
        void createRelationSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            when(assetRepository.findById(source.getId())).thenReturn(Optional.of(source));
            when(assetRepository.findById(target.getId())).thenReturn(Optional.of(target));
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .thenReturn(false);
            when(relationRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetRelation.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var result = assetService.createRelation(source.getId(), target.getId(), AssetRelationType.DEPENDS_ON);

            assertThat(result.getRelationType()).isEqualTo(AssetRelationType.DEPENDS_ON);
        }

        @Test
        void updateRelationSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntities(relation.getId())).thenReturn(Optional.of(relation));
            when(relationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var now = Instant.now();
            var command = new UpdateAssetRelationCommand("Updated description", "CMDB", "cmdb-123", now, "0.95");
            var result = assetService.updateRelation(source.getId(), relation.getId(), command);

            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getSourceSystem()).isEqualTo("CMDB");
            assertThat(result.getExternalSourceId()).isEqualTo("cmdb-123");
            assertThat(result.getCollectedAt()).isEqualTo(now);
            assertThat(result.getConfidence()).isEqualTo("0.95");
            assertThat(result.getRelationType()).isEqualTo(AssetRelationType.DEPENDS_ON);
            assertThat(result.getSource()).isSameAs(source);
            assertThat(result.getTarget()).isSameAs(target);
        }

        @Test
        void createRelationSelfReferenceThrows() {
            var id = UUID.randomUUID();

            assertThatThrownBy(() -> assetService.createRelation(id, id, AssetRelationType.DEPENDS_ON))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot relate to itself");
        }

        @Test
        void createRelationDuplicateThrowsConflict() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                            assetService.createRelation(source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void createRelationCrossProjectThrows() {
            var otherProject = new Project("other", "Other");
            var otherProjectId = UUID.randomUUID();
            setField(otherProject, "id", otherProjectId);

            var source = createAsset("ASSET-001", "Source");
            var target = new OperationalAsset(otherProject, "ASSET-002", "Target");
            setField(target, "id", UUID.randomUUID());

            when(assetRepository.findById(source.getId())).thenReturn(Optional.of(source));
            when(assetRepository.findById(target.getId())).thenReturn(Optional.of(target));
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                            assetService.createRelation(source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("different projects");
        }

        @Test
        void deleteRelationSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntities(relation.getId())).thenReturn(Optional.of(relation));

            assetService.deleteRelation(source.getId(), relation.getId());
            verify(relationRepository).delete(relation);
        }

        @Test
        void deleteRelationNotBelongingThrows() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var unrelated = createAsset("ASSET-003", "Unrelated");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntities(relation.getId())).thenReturn(Optional.of(relation));

            assertThatThrownBy(() -> assetService.deleteRelation(unrelated.getId(), relation.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void updateRelationNotBelongingThrows() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var unrelated = createAsset("ASSET-003", "Unrelated");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntities(relation.getId())).thenReturn(Optional.of(relation));

            assertThatThrownBy(() -> assetService.updateRelation(
                            unrelated.getId(),
                            relation.getId(),
                            new UpdateAssetRelationCommand("desc", null, null, null, null)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    @Nested
    class Links {

        private AssetLink makeLink(OperationalAsset asset) {
            var link = new AssetLink(asset, AssetLinkTargetType.REQUIREMENT, null, "GC-M010", AssetLinkType.IMPLEMENTS);
            setField(link, "id", UUID.randomUUID());
            setField(link, "createdAt", Instant.now());
            setField(link, "updatedAt", Instant.now());
            return link;
        }

        @Test
        void createLinkSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            asset.getId(), AssetLinkTargetType.REQUIREMENT, "GC-M010", AssetLinkType.IMPLEMENTS))
                    .thenReturn(false);
            when(graphTargetResolverService.validateAssetTarget(
                            projectId, AssetLinkTargetType.REQUIREMENT, null, "GC-M010"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "GC-M010", false));
            when(linkRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetLink.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.REQUIREMENT, null, "GC-M010", AssetLinkType.IMPLEMENTS, null, null);
            var result = assetService.createLink(asset.getId(), command);

            assertThat(result.getTargetType()).isEqualTo(AssetLinkTargetType.REQUIREMENT);
            assertThat(result.getTargetIdentifier()).isEqualTo("GC-M010");
            assertThat(result.getLinkType()).isEqualTo(AssetLinkType.IMPLEMENTS);
        }

        @Test
        void createLinkWithOptionalFields() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            asset.getId(), AssetLinkTargetType.EXTERNAL, "jira-123", AssetLinkType.DEPENDS_ON))
                    .thenReturn(false);
            when(graphTargetResolverService.validateAssetTarget(
                            projectId, AssetLinkTargetType.EXTERNAL, null, "jira-123"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "jira-123", false));
            when(linkRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetLink.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.EXTERNAL,
                    null,
                    "jira-123",
                    AssetLinkType.DEPENDS_ON,
                    "https://jira.example.com/123",
                    "External Dependency");
            var result = assetService.createLink(asset.getId(), command);

            assertThat(result.getTargetUrl()).isEqualTo("https://jira.example.com/123");
            assertThat(result.getTargetTitle()).isEqualTo("External Dependency");
        }

        @Test
        void createLinkDuplicateThrowsConflict() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            asset.getId(), AssetLinkTargetType.REQUIREMENT, "GC-M010", AssetLinkType.IMPLEMENTS))
                    .thenReturn(true);
            when(graphTargetResolverService.validateAssetTarget(
                            projectId, AssetLinkTargetType.REQUIREMENT, null, "GC-M010"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "GC-M010", false));

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.REQUIREMENT, null, "GC-M010", AssetLinkType.IMPLEMENTS, null, null);

            assertThatThrownBy(() -> assetService.createLink(asset.getId(), command))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void createLinkAssetNotFoundThrows() {
            var id = UUID.randomUUID();
            when(assetRepository.findById(id)).thenReturn(Optional.empty());

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.REQUIREMENT, null, "GC-M010", AssetLinkType.IMPLEMENTS, null, null);

            assertThatThrownBy(() -> assetService.createLink(id, command)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void getLinksForAssetReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(linkRepository.findByAssetId(asset.getId())).thenReturn(List.of(makeLink(asset)));

            var result = assetService.getLinksForAsset(asset.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTargetIdentifier()).isEqualTo("GC-M010");
        }

        @Test
        void deleteLinkSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            var link = makeLink(asset);
            when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

            assetService.deleteLink(asset.getId(), link.getId());
            verify(linkRepository).delete(link);
        }

        @Test
        void deleteLinkNotBelongingThrows() {
            var asset = createAsset("ASSET-001", "Web Server");
            var other = createAsset("ASSET-002", "Other");
            var link = makeLink(asset);
            when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> assetService.deleteLink(other.getId(), link.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void getLinksByTargetReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(linkRepository.findByTargetTypeAndTargetIdentifierAndProjectId(
                            AssetLinkTargetType.REQUIREMENT, "GC-M010", projectId))
                    .thenReturn(List.of(makeLink(asset)));

            var result = assetService.getLinksByTarget(projectId, AssetLinkTargetType.REQUIREMENT, null, "GC-M010");

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class ExternalIds {

        @Test
        void createExternalIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(externalIdRepository.existsByAssetIdAndSourceSystemAndSourceId(
                            asset.getId(), "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc"))
                    .thenReturn(false);
            when(externalIdRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetExternalId.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var command = new CreateAssetExternalIdCommand(
                    "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc", Instant.now(), "HIGH");
            var result = assetService.createExternalId(asset.getId(), command);

            assertThat(result.getSourceSystem()).isEqualTo("AWS");
            assertThat(result.getSourceId()).isEqualTo("arn:aws:ec2:us-east-1:123:instance/i-abc");
            assertThat(result.getConfidence()).isEqualTo("HIGH");
        }

        @Test
        void createExternalIdDuplicateThrowsConflict() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(externalIdRepository.existsByAssetIdAndSourceSystemAndSourceId(
                            asset.getId(), "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc"))
                    .thenReturn(true);

            var command =
                    new CreateAssetExternalIdCommand("AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc", null, null);

            assertThatThrownBy(() -> assetService.createExternalId(asset.getId(), command))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void updateExternalIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findByIdWithAsset(extId.getId())).thenReturn(Optional.of(extId));
            when(externalIdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var now = Instant.now();
            var command = new UpdateAssetExternalIdCommand(now, "MEDIUM");
            var result = assetService.updateExternalId(asset.getId(), extId.getId(), command);

            assertThat(result.getCollectedAt()).isEqualTo(now);
            assertThat(result.getConfidence()).isEqualTo("MEDIUM");
        }

        @Test
        void deleteExternalIdNotBelongingThrows() {
            var asset = createAsset("ASSET-001", "Web Server");
            var other = createAsset("ASSET-002", "Other");
            var extId = new AssetExternalId(asset, "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findByIdWithAsset(extId.getId())).thenReturn(Optional.of(extId));

            assertThatThrownBy(() -> assetService.deleteExternalId(other.getId(), extId.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void getExternalIdsReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(externalIdRepository.findByAssetId(asset.getId())).thenReturn(List.of(extId));

            var result = assetService.getExternalIds(asset.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceSystem()).isEqualTo("AWS");
        }

        @Test
        void deleteExternalIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findByIdWithAsset(extId.getId())).thenReturn(Optional.of(extId));

            assetService.deleteExternalId(asset.getId(), extId.getId());
            verify(externalIdRepository).delete(extId);
        }
    }

    @Nested
    class RelationProvenance {

        @Test
        void createRelationWithProvenanceSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            when(assetRepository.findById(source.getId())).thenReturn(Optional.of(source));
            when(assetRepository.findById(target.getId())).thenReturn(Optional.of(target));
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .thenReturn(false);
            when(relationRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetRelation.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var now = Instant.now();
            var command = new CreateAssetRelationCommand(
                    target.getId(),
                    AssetRelationType.DEPENDS_ON,
                    "Observed dependency",
                    "AWS_CONFIG",
                    "config-rule-123",
                    now,
                    "0.95");
            var result = assetService.createRelation(command, source.getId());

            assertThat(result.getRelationType()).isEqualTo(AssetRelationType.DEPENDS_ON);
            assertThat(result.getDescription()).isEqualTo("Observed dependency");
            assertThat(result.getSourceSystem()).isEqualTo("AWS_CONFIG");
            assertThat(result.getExternalSourceId()).isEqualTo("config-rule-123");
            assertThat(result.getCollectedAt()).isEqualTo(now);
            assertThat(result.getConfidence()).isEqualTo("0.95");
        }
    }

    @Nested
    class ProjectAwareUpdate {

        @Test
        void updateWithProjectIdSucceeds() {
            var asset = createAsset("ASSET-001", "Old Name");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateAssetCommand("New Name", "New desc", AssetType.DATABASE);
            var result = assetService.update(projectId, asset.getId(), command);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("New desc");
            assertThat(result.getAssetType()).isEqualTo(AssetType.DATABASE);
        }

        @Test
        void updateWithProjectIdNotFoundThrows() {
            var id = UUID.randomUUID();
            when(assetRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            var command = new UpdateAssetCommand("New Name", null, null);

            assertThatThrownBy(() -> assetService.update(projectId, id, command))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ProjectAwareRead {

        @Test
        void getByIdWithProjectIdReturnsAsset() {
            var asset = createAsset("ASSET-001", "Test");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));

            var result = assetService.getById(projectId, asset.getId());
            assertThat(result.getUid()).isEqualTo("ASSET-001");
        }

        @Test
        void getByIdWithProjectIdNotFoundThrows() {
            var id = UUID.randomUUID();
            when(assetRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.getById(projectId, id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void listByProjectAndTypeReturnsList() {
            var a1 = createAsset("ASSET-001", "DB One");
            a1.setAssetType(AssetType.DATABASE);
            when(assetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(projectId, AssetType.DATABASE))
                    .thenReturn(List.of(a1));

            var result = assetService.listByProjectAndType(projectId, AssetType.DATABASE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssetType()).isEqualTo(AssetType.DATABASE);
        }
    }

    @Nested
    class ProjectAwareArchive {

        @Test
        void archiveWithProjectIdSetsArchivedAt() {
            var asset = createAsset("ASSET-001", "Test");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = assetService.archive(projectId, asset.getId());
            assertThat(result.getArchivedAt()).isNotNull();
        }
    }

    @Nested
    class ProjectAwareDelete {

        @Test
        void deleteWithProjectIdSucceeds() {
            var asset = createAsset("ASSET-001", "Test");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));

            assetService.delete(projectId, asset.getId());
            verify(assetRepository).delete(asset);
        }
    }

    @Nested
    class ProjectAwareRelations {

        @Test
        void createRelationWithProjectIdSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            when(assetRepository.findByIdAndProjectId(source.getId(), projectId))
                    .thenReturn(Optional.of(source));
            when(assetRepository.findByIdAndProjectId(target.getId(), projectId))
                    .thenReturn(Optional.of(target));
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .thenReturn(false);
            when(relationRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetRelation.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var result = assetService.createRelation(
                    projectId, source.getId(), target.getId(), AssetRelationType.DEPENDS_ON);

            assertThat(result.getRelationType()).isEqualTo(AssetRelationType.DEPENDS_ON);
        }

        @Test
        void createRelationWithProjectIdSelfReferenceThrows() {
            var id = UUID.randomUUID();

            assertThatThrownBy(() -> assetService.createRelation(projectId, id, id, AssetRelationType.DEPENDS_ON))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot relate to itself");
        }

        @Test
        void createRelationWithProjectIdDuplicateThrowsConflict() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .thenReturn(true);

            assertThatThrownBy(() -> assetService.createRelation(
                            projectId, source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void createRelationWithCommandAndProjectIdSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            when(assetRepository.findByIdAndProjectId(source.getId(), projectId))
                    .thenReturn(Optional.of(source));
            when(assetRepository.findByIdAndProjectId(target.getId(), projectId))
                    .thenReturn(Optional.of(target));
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            source.getId(), target.getId(), AssetRelationType.DEPENDS_ON))
                    .thenReturn(false);
            when(relationRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetRelation.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var now = Instant.now();
            var command = new CreateAssetRelationCommand(
                    target.getId(), AssetRelationType.DEPENDS_ON, "desc", "SRC", "ext-1", now, "0.8");
            var result = assetService.createRelation(projectId, command, source.getId());

            assertThat(result.getRelationType()).isEqualTo(AssetRelationType.DEPENDS_ON);
            assertThat(result.getDescription()).isEqualTo("desc");
            assertThat(result.getSourceSystem()).isEqualTo("SRC");
            assertThat(result.getExternalSourceId()).isEqualTo("ext-1");
            assertThat(result.getCollectedAt()).isEqualTo(now);
            assertThat(result.getConfidence()).isEqualTo("0.8");
        }

        @Test
        void updateRelationWithProjectIdSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntitiesAndProjectId(relation.getId(), projectId))
                    .thenReturn(Optional.of(relation));
            when(relationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var now = Instant.now();
            var command = new UpdateAssetRelationCommand("Updated", "SYS", "ext-id", now, "0.75");
            var result = assetService.updateRelation(projectId, source.getId(), relation.getId(), command);

            assertThat(result.getDescription()).isEqualTo("Updated");
            assertThat(result.getSourceSystem()).isEqualTo("SYS");
            assertThat(result.getExternalSourceId()).isEqualTo("ext-id");
            assertThat(result.getCollectedAt()).isEqualTo(now);
            assertThat(result.getConfidence()).isEqualTo("0.75");
        }

        @Test
        void updateRelationWithProjectIdNotBelongingThrows() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var unrelated = createAsset("ASSET-003", "Unrelated");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntitiesAndProjectId(relation.getId(), projectId))
                    .thenReturn(Optional.of(relation));

            assertThatThrownBy(() -> assetService.updateRelation(
                            projectId,
                            unrelated.getId(),
                            relation.getId(),
                            new UpdateAssetRelationCommand("desc", null, null, null, null)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void getRelationsWithProjectIdReturnsCombinedList() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var outgoing = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(outgoing, "id", UUID.randomUUID());
            var incoming = new AssetRelation(target, source, AssetRelationType.CONTAINS);
            setField(incoming, "id", UUID.randomUUID());

            when(assetRepository.findByIdAndProjectId(source.getId(), projectId))
                    .thenReturn(Optional.of(source));
            when(relationRepository.findBySourceIdWithEntities(source.getId())).thenReturn(List.of(outgoing));
            when(relationRepository.findByTargetIdWithEntities(source.getId())).thenReturn(List.of(incoming));

            var result = assetService.getRelations(projectId, source.getId());

            assertThat(result).hasSize(2);
        }

        @Test
        void deleteRelationWithProjectIdSucceeds() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntitiesAndProjectId(relation.getId(), projectId))
                    .thenReturn(Optional.of(relation));

            assetService.deleteRelation(projectId, source.getId(), relation.getId());
            verify(relationRepository).delete(relation);
        }

        @Test
        void deleteRelationWithProjectIdNotBelongingThrows() {
            var source = createAsset("ASSET-001", "Source");
            var target = createAsset("ASSET-002", "Target");
            var unrelated = createAsset("ASSET-003", "Unrelated");
            var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
            setField(relation, "id", UUID.randomUUID());

            when(relationRepository.findByIdWithEntitiesAndProjectId(relation.getId(), projectId))
                    .thenReturn(Optional.of(relation));

            assertThatThrownBy(() -> assetService.deleteRelation(projectId, unrelated.getId(), relation.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    @Nested
    class ProjectAwareLinks {

        private AssetLink makeLink(OperationalAsset asset) {
            var link = new AssetLink(asset, AssetLinkTargetType.REQUIREMENT, null, "GC-M010", AssetLinkType.IMPLEMENTS);
            setField(link, "id", UUID.randomUUID());
            setField(link, "createdAt", Instant.now());
            setField(link, "updatedAt", Instant.now());
            return link;
        }

        @Test
        void createLinkWithProjectIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(graphTargetResolverService.validateAssetTarget(
                            projectId, AssetLinkTargetType.REQUIREMENT, null, "GC-M010"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "GC-M010", false));
            when(linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            asset.getId(), AssetLinkTargetType.REQUIREMENT, "GC-M010", AssetLinkType.IMPLEMENTS))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetLink.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.REQUIREMENT, null, "GC-M010", AssetLinkType.IMPLEMENTS, null, null);
            var result = assetService.createLink(projectId, asset.getId(), command);

            assertThat(result.getTargetType()).isEqualTo(AssetLinkTargetType.REQUIREMENT);
            assertThat(result.getTargetIdentifier()).isEqualTo("GC-M010");
        }

        @Test
        void createLinkWithProjectIdInternalTargetSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            var targetEntityId = UUID.randomUUID();
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(graphTargetResolverService.validateAssetTarget(
                            projectId, AssetLinkTargetType.REQUIREMENT, targetEntityId, "GC-M010"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(targetEntityId, "GC-M010", true));
            when(linkRepository.existsByAssetIdAndTargetTypeAndTargetEntityIdAndLinkType(
                            asset.getId(), AssetLinkTargetType.REQUIREMENT, targetEntityId, AssetLinkType.IMPLEMENTS))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetLink.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.REQUIREMENT, targetEntityId, "GC-M010", AssetLinkType.IMPLEMENTS, null, null);
            var result = assetService.createLink(projectId, asset.getId(), command);

            assertThat(result.getTargetType()).isEqualTo(AssetLinkTargetType.REQUIREMENT);
        }

        @Test
        void createLinkWithProjectIdDuplicateInternalTargetThrowsConflict() {
            var asset = createAsset("ASSET-001", "Web Server");
            var targetEntityId = UUID.randomUUID();
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(graphTargetResolverService.validateAssetTarget(
                            projectId, AssetLinkTargetType.REQUIREMENT, targetEntityId, "GC-M010"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(targetEntityId, "GC-M010", true));
            when(linkRepository.existsByAssetIdAndTargetTypeAndTargetEntityIdAndLinkType(
                            asset.getId(), AssetLinkTargetType.REQUIREMENT, targetEntityId, AssetLinkType.IMPLEMENTS))
                    .thenReturn(true);

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.REQUIREMENT, targetEntityId, "GC-M010", AssetLinkType.IMPLEMENTS, null, null);

            assertThatThrownBy(() -> assetService.createLink(projectId, asset.getId(), command))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void createLinkWithProjectIdSetsOptionalFields() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(graphTargetResolverService.validateAssetTarget(
                            projectId, AssetLinkTargetType.EXTERNAL, null, "ext-123"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "ext-123", false));
            when(linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            asset.getId(), AssetLinkTargetType.EXTERNAL, "ext-123", AssetLinkType.DEPENDS_ON))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetLink.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var command = new CreateAssetLinkCommand(
                    AssetLinkTargetType.EXTERNAL,
                    null,
                    "ext-123",
                    AssetLinkType.DEPENDS_ON,
                    "https://example.com",
                    "Example");
            var result = assetService.createLink(projectId, asset.getId(), command);

            assertThat(result.getTargetUrl()).isEqualTo("https://example.com");
            assertThat(result.getTargetTitle()).isEqualTo("Example");
        }

        @Test
        void getLinksForAssetWithProjectIdReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(linkRepository.findByAssetId(asset.getId())).thenReturn(List.of(makeLink(asset)));

            var result = assetService.getLinksForAsset(projectId, asset.getId());

            assertThat(result).hasSize(1);
        }

        @Test
        void getLinksForAssetByTargetTypeReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(linkRepository.findByAssetIdAndTargetType(asset.getId(), AssetLinkTargetType.REQUIREMENT))
                    .thenReturn(List.of(makeLink(asset)));

            var result = assetService.getLinksForAssetByTargetType(
                    projectId, asset.getId(), AssetLinkTargetType.REQUIREMENT);

            assertThat(result).hasSize(1);
        }

        @Test
        void getLinksForAssetByTargetTypeLegacyReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(linkRepository.findByAssetIdAndTargetType(asset.getId(), AssetLinkTargetType.REQUIREMENT))
                    .thenReturn(List.of(makeLink(asset)));

            var result = assetService.getLinksForAssetByTargetType(asset.getId(), AssetLinkTargetType.REQUIREMENT);

            assertThat(result).hasSize(1);
        }

        @Test
        void getLinksByTargetWithEntityIdReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            var targetEntityId = UUID.randomUUID();
            when(linkRepository.findByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.REQUIREMENT, targetEntityId, projectId))
                    .thenReturn(List.of(makeLink(asset)));

            var result = assetService.getLinksByTarget(
                    projectId, AssetLinkTargetType.REQUIREMENT, targetEntityId, "GC-M010");

            assertThat(result).hasSize(1);
        }

        @Test
        void deleteLinkWithProjectIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            var link = makeLink(asset);
            when(linkRepository.findByIdWithAssetAndProjectId(link.getId(), projectId))
                    .thenReturn(Optional.of(link));

            assetService.deleteLink(projectId, asset.getId(), link.getId());
            verify(linkRepository).delete(link);
        }

        @Test
        void deleteLinkWithProjectIdNotBelongingThrows() {
            var asset = createAsset("ASSET-001", "Web Server");
            var other = createAsset("ASSET-002", "Other");
            var link = makeLink(asset);
            when(linkRepository.findByIdWithAssetAndProjectId(link.getId(), projectId))
                    .thenReturn(Optional.of(link));

            assertThatThrownBy(() -> assetService.deleteLink(projectId, other.getId(), link.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void deleteLinkWithProjectIdNotFoundThrows() {
            var linkId = UUID.randomUUID();
            when(linkRepository.findByIdWithAssetAndProjectId(linkId, projectId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.deleteLink(projectId, UUID.randomUUID(), linkId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ProjectAwareExternalIds {

        @Test
        void createExternalIdWithProjectIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(externalIdRepository.existsByAssetIdAndSourceSystemAndSourceId(asset.getId(), "AWS", "i-abc"))
                    .thenReturn(false);
            when(externalIdRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, AssetExternalId.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var now = Instant.now();
            var command = new CreateAssetExternalIdCommand("AWS", "i-abc", now, "HIGH");
            var result = assetService.createExternalId(projectId, asset.getId(), command);

            assertThat(result.getSourceSystem()).isEqualTo("AWS");
            assertThat(result.getSourceId()).isEqualTo("i-abc");
            assertThat(result.getCollectedAt()).isEqualTo(now);
            assertThat(result.getConfidence()).isEqualTo("HIGH");
        }

        @Test
        void createExternalIdWithProjectIdDuplicateThrowsConflict() {
            var asset = createAsset("ASSET-001", "Web Server");
            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(externalIdRepository.existsByAssetIdAndSourceSystemAndSourceId(asset.getId(), "AWS", "i-abc"))
                    .thenReturn(true);

            var command = new CreateAssetExternalIdCommand("AWS", "i-abc", null, null);

            assertThatThrownBy(() -> assetService.createExternalId(projectId, asset.getId(), command))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void updateExternalIdWithProjectIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findByIdWithAssetAndProjectId(extId.getId(), projectId))
                    .thenReturn(Optional.of(extId));
            when(externalIdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var now = Instant.now();
            var command = new UpdateAssetExternalIdCommand(now, "LOW");
            var result = assetService.updateExternalId(projectId, asset.getId(), extId.getId(), command);

            assertThat(result.getCollectedAt()).isEqualTo(now);
            assertThat(result.getConfidence()).isEqualTo("LOW");
        }

        @Test
        void updateExternalIdWithProjectIdNotBelongingThrows() {
            var asset = createAsset("ASSET-001", "Web Server");
            var other = createAsset("ASSET-002", "Other");
            var extId = new AssetExternalId(asset, "AWS", "i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findByIdWithAssetAndProjectId(extId.getId(), projectId))
                    .thenReturn(Optional.of(extId));

            assertThatThrownBy(() -> assetService.updateExternalId(
                            projectId, other.getId(), extId.getId(), new UpdateAssetExternalIdCommand(null, "LOW")))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void getExternalIdsWithProjectIdReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(externalIdRepository.findByAssetId(asset.getId())).thenReturn(List.of(extId));

            var result = assetService.getExternalIds(projectId, asset.getId());

            assertThat(result).hasSize(1);
        }

        @Test
        void getExternalIdsBySourceWithProjectIdReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(assetRepository.findByIdAndProjectId(asset.getId(), projectId)).thenReturn(Optional.of(asset));
            when(externalIdRepository.findByAssetIdAndSourceSystem(asset.getId(), "AWS"))
                    .thenReturn(List.of(extId));

            var result = assetService.getExternalIdsBySource(projectId, asset.getId(), "AWS");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceSystem()).isEqualTo("AWS");
        }

        @Test
        void getExternalIdsBySourceLegacyReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "GCP", "proj/inst");
            setField(extId, "id", UUID.randomUUID());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(externalIdRepository.findByAssetIdAndSourceSystem(asset.getId(), "GCP"))
                    .thenReturn(List.of(extId));

            var result = assetService.getExternalIdsBySource(asset.getId(), "GCP");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceSystem()).isEqualTo("GCP");
        }

        @Test
        void findByExternalIdReturnsList() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findBySourceSystemAndSourceIdAndProjectId("AWS", "i-abc", projectId))
                    .thenReturn(List.of(extId));

            var result = assetService.findByExternalId(projectId, "AWS", "i-abc");

            assertThat(result).hasSize(1);
        }

        @Test
        void deleteExternalIdWithProjectIdSucceeds() {
            var asset = createAsset("ASSET-001", "Web Server");
            var extId = new AssetExternalId(asset, "AWS", "i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findByIdWithAssetAndProjectId(extId.getId(), projectId))
                    .thenReturn(Optional.of(extId));

            assetService.deleteExternalId(projectId, asset.getId(), extId.getId());
            verify(externalIdRepository).delete(extId);
        }

        @Test
        void deleteExternalIdWithProjectIdNotBelongingThrows() {
            var asset = createAsset("ASSET-001", "Web Server");
            var other = createAsset("ASSET-002", "Other");
            var extId = new AssetExternalId(asset, "AWS", "i-abc");
            setField(extId, "id", UUID.randomUUID());

            when(externalIdRepository.findByIdWithAssetAndProjectId(extId.getId(), projectId))
                    .thenReturn(Optional.of(extId));

            assertThatThrownBy(() -> assetService.deleteExternalId(projectId, other.getId(), extId.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void deleteExternalIdWithProjectIdNotFoundThrows() {
            var extIdId = UUID.randomUUID();
            when(externalIdRepository.findByIdWithAssetAndProjectId(extIdId, projectId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.deleteExternalId(projectId, UUID.randomUUID(), extIdId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}

package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.assets.AssetController;
import com.keplerops.groundcontrol.domain.assets.model.AssetExternalId;
import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.service.AssetCycleEdge;
import com.keplerops.groundcontrol.domain.assets.service.AssetCycleResult;
import com.keplerops.groundcontrol.domain.assets.service.AssetService;
import com.keplerops.groundcontrol.domain.assets.service.AssetSubgraphResult;
import com.keplerops.groundcontrol.domain.assets.service.AssetTopologyService;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetCommand;
import com.keplerops.groundcontrol.domain.assets.service.UpdateAssetCommand;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AssetController.class)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private AssetTopologyService topologyService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private OperationalAsset makeAsset() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var asset = new OperationalAsset(project, "ASSET-001", "Web Server");
        asset.setAssetType(AssetType.SERVICE);
        asset.setDescription("A web server");
        setField(asset, "id", ASSET_ID);
        setField(asset, "createdAt", Instant.now());
        setField(asset, "updatedAt", Instant.now());
        return asset;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.create(any())).thenReturn(makeAsset());

        mockMvc.perform(
                        post("/api/v1/assets")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"uid":"ASSET-001","name":"Web Server","description":"A web server","assetType":"SERVICE"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is("ASSET-001")))
                .andExpect(jsonPath("$.name", is("Web Server")))
                .andExpect(jsonPath("$.assetType", is("SERVICE")))
                .andExpect(jsonPath("$.projectIdentifier", is("ground-control")));
    }

    @Test
    void listReturnsAssets() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.listByProject(PROJECT_ID)).thenReturn(List.of(makeAsset()));

        mockMvc.perform(get("/api/v1/assets").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("ASSET-001")));
    }

    @Test
    void createPersistsOwnershipCriticalityScopeMetadata() throws Exception {
        // GC-M012: POST body carries ownership/criticality/scope fields. Use
        // ArgumentCaptor on the CreateAssetCommand so we verify the controller
        // actually maps each request field into the command — a plain `any()`
        // would still pass if the controller dropped the new field reads.
        var enriched = makeAsset();
        enriched.setOwner("alice@example.com");
        enriched.setSteward("platform-sre");
        enriched.setEnvironment(com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment.PRODUCTION);
        enriched.setCriticality(com.keplerops.groundcontrol.domain.assets.state.AssetCriticality.CRITICAL);
        enriched.setBusinessContext("PCI scope");
        enriched.setScopeDesignation(com.keplerops.groundcontrol.domain.assets.state.AssetScope.IN_SCOPE);
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.create(any())).thenReturn(enriched);

        mockMvc.perform(
                        post("/api/v1/assets")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "uid":"ASSET-PCI",
                                          "name":"Payments API",
                                          "assetType":"SERVICE",
                                          "owner":"alice@example.com",
                                          "steward":"platform-sre",
                                          "environment":"PRODUCTION",
                                          "criticality":"CRITICAL",
                                          "businessContext":"PCI scope",
                                          "scopeDesignation":"IN_SCOPE"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner", is("alice@example.com")))
                .andExpect(jsonPath("$.steward", is("platform-sre")))
                .andExpect(jsonPath("$.environment", is("PRODUCTION")))
                .andExpect(jsonPath("$.criticality", is("CRITICAL")))
                .andExpect(jsonPath("$.businessContext", is("PCI scope")))
                .andExpect(jsonPath("$.scopeDesignation", is("IN_SCOPE")));

        var captor = ArgumentCaptor.forClass(CreateAssetCommand.class);
        verify(assetService).create(captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.owner()).isEqualTo("alice@example.com");
        org.assertj.core.api.Assertions.assertThat(cmd.steward()).isEqualTo("platform-sre");
        org.assertj.core.api.Assertions.assertThat(cmd.environment())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment.PRODUCTION);
        org.assertj.core.api.Assertions.assertThat(cmd.criticality())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.AssetCriticality.CRITICAL);
        org.assertj.core.api.Assertions.assertThat(cmd.businessContext()).isEqualTo("PCI scope");
        org.assertj.core.api.Assertions.assertThat(cmd.scopeDesignation())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.AssetScope.IN_SCOPE);
    }

    @Test
    void listSupportsOwnershipCriticalityScopeFilters() throws Exception {
        // GC-M012 + GC-M011: list endpoint routes through listByProjectAndFilters
        // when any of the filter query parameters is supplied, including the
        // GC-M011 `subtype` facet.
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.listByProjectAndFilters(
                        PROJECT_ID,
                        null,
                        "alice@example.com",
                        null,
                        com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment.PRODUCTION,
                        com.keplerops.groundcontrol.domain.assets.state.AssetCriticality.CRITICAL,
                        com.keplerops.groundcontrol.domain.assets.state.AssetScope.IN_SCOPE,
                        "aws_ec2",
                        null))
                .thenReturn(List.of(makeAsset()));

        mockMvc.perform(get("/api/v1/assets")
                        .param("project", "ground-control")
                        .param("owner", "alice@example.com")
                        .param("environment", "PRODUCTION")
                        .param("criticality", "CRITICAL")
                        .param("scope", "IN_SCOPE")
                        .param("subtype", "aws_ec2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void listSupportsSubtypeAloneFilter() throws Exception {
        // GC-M011: subtype alone is a valid filter facet on the canonical
        // list path; routes through listByProjectAndFilters(... subtype).
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.listByProjectAndFilters(PROJECT_ID, null, null, null, null, null, null, "aws_ec2", null))
                .thenReturn(List.of(makeAsset()));

        mockMvc.perform(get("/api/v1/assets").param("project", "ground-control").param("subtype", "aws_ec2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createForwardsKnowledgeStateIntoCommand() throws Exception {
        // GC-M018: knowledgeState arrives on the request body, lands on
        // the CreateAssetCommand, AND round-trips through AssetResponse so
        // the controller↔response wiring is verified end-to-end. Without
        // the $.knowledgeState jsonPath assertion, a regression that
        // dropped the field from AssetResponse.from() (or mapped it to
        // null) would leave this test green because the mock return value
        // is independent of the request body (test-quality review #906).
        var stub = makeAsset();
        stub.setKnowledgeState(com.keplerops.groundcontrol.domain.assets.state.KnowledgeState.PROVISIONAL);
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.create(any())).thenReturn(stub);

        mockMvc.perform(
                        post("/api/v1/assets")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "uid":"ASSET-NEW",
                                  "name":"Tentative Service",
                                  "knowledgeState":"PROVISIONAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.knowledgeState", is("PROVISIONAL")));

        var captor = ArgumentCaptor.forClass(CreateAssetCommand.class);
        verify(assetService).create(captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.knowledgeState())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.KnowledgeState.PROVISIONAL);
    }

    @Test
    void updateForwardsKnowledgeStateIntoCommand() throws Exception {
        // GC-M018: PUT body's knowledgeState lands on UpdateAssetCommand
        // AND round-trips through AssetResponse. Same wiring contract as
        // createForwardsKnowledgeStateIntoCommand.
        var stub = makeAsset();
        stub.setKnowledgeState(com.keplerops.groundcontrol.domain.assets.state.KnowledgeState.CONFIRMED);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.update(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(stub);

        mockMvc.perform(
                        put("/api/v1/assets/{id}", ASSET_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"knowledgeState":"CONFIRMED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeState", is("CONFIRMED")));

        var captor = ArgumentCaptor.forClass(UpdateAssetCommand.class);
        verify(assetService).update(eq(PROJECT_ID), eq(ASSET_ID), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().knowledgeState())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.KnowledgeState.CONFIRMED);
    }

    @Test
    void listSupportsKnowledgeStateFilter() throws Exception {
        // GC-M018: knowledgeState is the explicit confirmed-vs-provisional
        // filter knob the requirement says risk / threat / control workflows
        // must be able to use. Routes through listByProjectAndFilters(...,
        // knowledgeState).
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.listByProjectAndFilters(
                        PROJECT_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        com.keplerops.groundcontrol.domain.assets.state.KnowledgeState.CONFIRMED))
                .thenReturn(List.of(makeAsset()));

        mockMvc.perform(get("/api/v1/assets").param("project", "ground-control").param("knowledgeState", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getByIdReturnsAsset() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getById(PROJECT_ID, ASSET_ID)).thenReturn(makeAsset());

        mockMvc.perform(get("/api/v1/assets/{id}", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("ASSET-001")));
    }

    @Test
    void getByUidReturnsAsset() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getByUid(PROJECT_ID, "ASSET-001")).thenReturn(makeAsset());

        mockMvc.perform(get("/api/v1/assets/uid/{uid}", "ASSET-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("ASSET-001")));
    }

    @Test
    void updateReturnsUpdated() throws Exception {
        var updated = makeAsset();
        updated.setName("Updated Server");
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.update(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/assets/{id}", ASSET_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"name":"Updated Server"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Server")));
    }

    @Test
    void updateMapsClearFlagsAndMetadataIntoCommand() throws Exception {
        // GC-M012: the PUT body's clear flags + metadata must land on the
        // UpdateAssetCommand. Without ArgumentCaptor, the controller could
        // drop / transpose any of the six clear booleans (or any of the
        // metadata fields) and the test would still pass because the mock
        // return value is independent of the request shape.
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.update(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(makeAsset());

        mockMvc.perform(
                        put("/api/v1/assets/{id}", ASSET_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "owner":"alice@example.com",
                                  "criticality":"CRITICAL",
                                  "scopeDesignation":"IN_SCOPE",
                                  "clearSteward":true,
                                  "clearEnvironment":true,
                                  "clearBusinessContext":true
                                }
                                """))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(UpdateAssetCommand.class);
        verify(assetService).update(eq(PROJECT_ID), eq(ASSET_ID), captor.capture());
        var cmd = captor.getValue();
        // Assigned metadata flows through.
        org.assertj.core.api.Assertions.assertThat(cmd.owner()).isEqualTo("alice@example.com");
        org.assertj.core.api.Assertions.assertThat(cmd.criticality())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.AssetCriticality.CRITICAL);
        org.assertj.core.api.Assertions.assertThat(cmd.scopeDesignation())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.AssetScope.IN_SCOPE);
        // Clear flags that the payload set must be true; the others must be
        // false so a transposition (e.g. clearSteward wired to clearOwner)
        // fails the test.
        org.assertj.core.api.Assertions.assertThat(cmd.clearSteward()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cmd.clearEnvironment()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cmd.clearBusinessContext()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cmd.clearOwner()).isFalse();
        org.assertj.core.api.Assertions.assertThat(cmd.clearCriticality()).isFalse();
        org.assertj.core.api.Assertions.assertThat(cmd.clearScopeDesignation()).isFalse();
    }

    @Test
    void createCarriesSubtypeAndMetadata() throws Exception {
        // GC-M011: subtype + metadata land on CreateAssetCommand and serialize
        // through the response. ArgumentCaptor proves the controller wire-up,
        // not just that the mock returns a plausible response.
        var enriched = makeAsset();
        enriched.setSubtype("aws_ec2");
        enriched.setMetadata(java.util.Map.of("cloud_account_id", "123456", "region", "us-west-2"));
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.create(any())).thenReturn(enriched);

        mockMvc.perform(
                        post("/api/v1/assets")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "uid":"ASSET-EC2",
                                          "name":"EC2 worker",
                                          "assetType":"WORKLOAD",
                                          "subtype":"aws_ec2",
                                          "metadata":{"cloud_account_id":"123456","region":"us-west-2"}
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtype", is("aws_ec2")))
                .andExpect(jsonPath("$.metadata.cloud_account_id", is("123456")))
                .andExpect(jsonPath("$.metadata.region", is("us-west-2")));

        var captor = ArgumentCaptor.forClass(CreateAssetCommand.class);
        verify(assetService).create(captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.subtype()).isEqualTo("aws_ec2");
        org.assertj.core.api.Assertions.assertThat(cmd.metadata())
                .containsEntry("cloud_account_id", "123456")
                .containsEntry("region", "us-west-2");
    }

    @Test
    void updateMapsSubtypeAndMetadataClearFlags() throws Exception {
        // GC-M011: PUT body's subtype/metadata + clearSubtype/clearMetadata
        // land on UpdateAssetCommand. The captor proves the new wire entries
        // aren't dropped; a transposition (e.g. clearSubtype wired to
        // clearMetadata) fails the test.
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.update(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(makeAsset());

        mockMvc.perform(
                        put("/api/v1/assets/{id}", ASSET_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "subtype":"service_principal",
                                          "metadata":{"client_id":"abc"},
                                          "clearMetadata":false,
                                          "clearSubtype":false
                                        }
                                        """))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(UpdateAssetCommand.class);
        verify(assetService).update(eq(PROJECT_ID), eq(ASSET_ID), captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.subtype()).isEqualTo("service_principal");
        org.assertj.core.api.Assertions.assertThat(cmd.metadata()).containsEntry("client_id", "abc");
        org.assertj.core.api.Assertions.assertThat(cmd.clearSubtype()).isFalse();
        org.assertj.core.api.Assertions.assertThat(cmd.clearMetadata()).isFalse();
    }

    @Test
    void updateClearsSubtypeAndMetadata() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.update(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(makeAsset());

        mockMvc.perform(
                        put("/api/v1/assets/{id}", ASSET_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"clearSubtype":true,"clearMetadata":true}
                                        """))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(UpdateAssetCommand.class);
        verify(assetService).update(eq(PROJECT_ID), eq(ASSET_ID), captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.clearSubtype()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cmd.clearMetadata()).isTrue();
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/assets/{id}", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(assetService).delete(PROJECT_ID, ASSET_ID);
    }

    @Test
    void archiveReturnsAsset() throws Exception {
        var archived = makeAsset();
        setField(archived, "archivedAt", Instant.now());
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.archive(PROJECT_ID, ASSET_ID)).thenReturn(archived);

        mockMvc.perform(post("/api/v1/assets/{id}/archive", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
    }

    @Test
    void createRelationReturns201() throws Exception {
        var source = makeAsset();
        var target = makeAsset();
        setField(target, "id", UUID.randomUUID());
        target.setName("Database");

        var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.now());
        setField(relation, "updatedAt", Instant.now());
        relation.setDescription("Observed dependency");

        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.createRelation(
                        eq(PROJECT_ID),
                        any(com.keplerops.groundcontrol.domain.assets.service.CreateAssetRelationCommand.class),
                        eq(ASSET_ID)))
                .thenReturn(relation);

        mockMvc.perform(post("/api/v1/assets/{id}/relations", ASSET_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                {"targetId":"%s","relationType":"DEPENDS_ON","description":"Observed dependency"}
                """
                                        .formatted(target.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationType", is("DEPENDS_ON")))
                .andExpect(jsonPath("$.description", is("Observed dependency")))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void getRelationsReturnsList() throws Exception {
        var source = makeAsset();
        var target = makeAsset();
        setField(target, "id", UUID.randomUUID());

        var relation = new AssetRelation(source, target, AssetRelationType.COMMUNICATES_WITH);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.now());
        setField(relation, "updatedAt", Instant.now());
        relation.setDescription("Service communication");

        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getRelations(PROJECT_ID, ASSET_ID)).thenReturn(List.of(relation));

        mockMvc.perform(get("/api/v1/assets/{id}/relations", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].relationType", is("COMMUNICATES_WITH")))
                .andExpect(jsonPath("$[0].description", is("Service communication")))
                .andExpect(jsonPath("$[0].updatedAt").isNotEmpty());
    }

    @Test
    void updateRelationReturns200() throws Exception {
        var source = makeAsset();
        var target = makeAsset();
        setField(target, "id", UUID.randomUUID());

        var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.now());
        setField(relation, "updatedAt", Instant.now());
        relation.setDescription("Refined dependency");
        relation.setSourceSystem("CMDB");
        relation.setConfidence("0.95");

        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.updateRelation(
                        eq(PROJECT_ID),
                        eq(ASSET_ID),
                        eq(relation.getId()),
                        any(com.keplerops.groundcontrol.domain.assets.service.UpdateAssetRelationCommand.class)))
                .thenReturn(relation);

        mockMvc.perform(
                        put("/api/v1/assets/{id}/relations/{relationId}", ASSET_ID, relation.getId())
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"description":"Refined dependency","sourceSystem":"CMDB","confidence":"0.95"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Refined dependency")))
                .andExpect(jsonPath("$.sourceSystem", is("CMDB")))
                .andExpect(jsonPath("$.confidence", is("0.95")))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void detectCyclesReturnsList() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        var cycle = new AssetCycleResult(
                List.of("A", "B", "A"),
                List.of(
                        new AssetCycleEdge("A", "B", AssetRelationType.DEPENDS_ON),
                        new AssetCycleEdge("B", "A", AssetRelationType.DEPENDS_ON)));
        when(topologyService.detectCycles(PROJECT_ID)).thenReturn(List.of(cycle));

        mockMvc.perform(get("/api/v1/assets/topology/cycles").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberUids", hasSize(3)));
    }

    @Test
    void impactAnalysisReturnsList() throws Exception {
        var asset = makeAsset();
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(topologyService.impactAnalysis(PROJECT_ID, ASSET_ID)).thenReturn(Set.of(asset));

        mockMvc.perform(get("/api/v1/assets/{id}/topology/impact", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void extractSubgraphReturnsResult() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        var result = new AssetSubgraphResult(List.of(makeAsset()), List.of());
        when(topologyService.extractSubgraph(eq(PROJECT_ID), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/assets/topology/subgraph")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"rootUids":["ASSET-001"]}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets", hasSize(1)))
                .andExpect(jsonPath("$.relations", hasSize(0)));
    }

    // --- Asset Link tests ---

    private AssetLink makeLink() {
        var asset = makeAsset();
        var link = new AssetLink(asset, AssetLinkTargetType.REQUIREMENT, null, "GC-M010", AssetLinkType.IMPLEMENTS);
        setField(link, "id", UUID.randomUUID());
        setField(link, "createdAt", Instant.now());
        setField(link, "updatedAt", Instant.now());
        return link;
    }

    @Test
    void createLinkReturns201() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.createLink(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(makeLink());

        mockMvc.perform(
                        post("/api/v1/assets/{id}/links", ASSET_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"targetType":"REQUIREMENT","targetIdentifier":"GC-M010","linkType":"IMPLEMENTS"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType", is("REQUIREMENT")))
                .andExpect(jsonPath("$.targetIdentifier", is("GC-M010")))
                .andExpect(jsonPath("$.linkType", is("IMPLEMENTS")));
    }

    @Test
    void getLinksReturnsList() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getLinksForAsset(PROJECT_ID, ASSET_ID)).thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/assets/{id}/links", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType", is("REQUIREMENT")));
    }

    @Test
    void getLinksWithTargetTypeFilter() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getLinksForAssetByTargetType(PROJECT_ID, ASSET_ID, AssetLinkTargetType.REQUIREMENT))
                .thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/assets/{id}/links", ASSET_ID)
                        .param("project", "ground-control")
                        .param("target_type", "REQUIREMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deleteLinkReturns204() throws Exception {
        var linkId = UUID.randomUUID();
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/assets/{id}/links/{linkId}", ASSET_ID, linkId)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(assetService).deleteLink(PROJECT_ID, ASSET_ID, linkId);
    }

    @Test
    void getLinksByTargetReturnsList() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getLinksByTarget(PROJECT_ID, AssetLinkTargetType.REQUIREMENT, null, "GC-M010"))
                .thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/assets/links/by-target")
                        .param("target_type", "REQUIREMENT")
                        .param("target_identifier", "GC-M010")
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assetUid", is("ASSET-001")));
    }

    // --- External ID tests ---

    private AssetExternalId makeExternalId() {
        var asset = makeAsset();
        var extId = new AssetExternalId(asset, "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc");
        extId.setConfidence("HIGH");
        extId.setCollectedAt(Instant.parse("2026-03-30T12:00:00Z"));
        setField(extId, "id", UUID.randomUUID());
        setField(extId, "createdAt", Instant.now());
        setField(extId, "updatedAt", Instant.now());
        return extId;
    }

    @Test
    void createExternalIdReturns201() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.createExternalId(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(makeExternalId());

        mockMvc.perform(
                        post("/api/v1/assets/{id}/external-ids", ASSET_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"sourceSystem":"AWS","sourceId":"arn:aws:ec2:us-east-1:123:instance/i-abc","confidence":"HIGH"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceSystem", is("AWS")))
                .andExpect(jsonPath("$.sourceId", is("arn:aws:ec2:us-east-1:123:instance/i-abc")))
                .andExpect(jsonPath("$.confidence", is("HIGH")));
    }

    @Test
    void getExternalIdsReturnsList() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getExternalIds(PROJECT_ID, ASSET_ID)).thenReturn(List.of(makeExternalId()));

        mockMvc.perform(get("/api/v1/assets/{id}/external-ids", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceSystem", is("AWS")));
    }

    @Test
    void getExternalIdsBySourceFilter() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getExternalIdsBySource(PROJECT_ID, ASSET_ID, "AWS")).thenReturn(List.of(makeExternalId()));

        mockMvc.perform(get("/api/v1/assets/{id}/external-ids", ASSET_ID)
                        .param("project", "ground-control")
                        .param("source_system", "AWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceSystem", is("AWS")));
    }

    @Test
    void updateExternalIdReturnsUpdated() throws Exception {
        var updated = makeExternalId();
        updated.setConfidence("MEDIUM");
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.updateExternalId(eq(PROJECT_ID), eq(ASSET_ID), any(), any()))
                .thenReturn(updated);

        var extIdId = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/assets/{id}/external-ids/{extIdId}", ASSET_ID, extIdId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"confidence":"MEDIUM"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence", is("MEDIUM")));
    }

    @Test
    void deleteExternalIdReturns204() throws Exception {
        var extIdId = UUID.randomUUID();
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/assets/{id}/external-ids/{extIdId}", ASSET_ID, extIdId)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(assetService).deleteExternalId(PROJECT_ID, ASSET_ID, extIdId);
    }

    @Test
    void findByExternalIdReturnsList() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.findByExternalId(PROJECT_ID, "AWS", "arn:aws:ec2:us-east-1:123:instance/i-abc"))
                .thenReturn(List.of(makeExternalId()));

        mockMvc.perform(get("/api/v1/assets/external-ids/by-source")
                        .param("source_system", "AWS")
                        .param("source_id", "arn:aws:ec2:us-east-1:123:instance/i-abc")
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assetUid", is("ASSET-001")));
    }

    @Test
    void createRelationWithProvenanceReturns201() throws Exception {
        var source = makeAsset();
        var target = makeAsset();
        setField(target, "id", UUID.randomUUID());
        target.setName("Database");

        var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.now());
        setField(relation, "updatedAt", Instant.now());
        relation.setDescription("Observed dependency");
        relation.setSourceSystem("AWS_CONFIG");
        relation.setConfidence("0.95");

        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.createRelation(
                        eq(PROJECT_ID),
                        any(com.keplerops.groundcontrol.domain.assets.service.CreateAssetRelationCommand.class),
                        eq(ASSET_ID)))
                .thenReturn(relation);

        mockMvc.perform(post("/api/v1/assets/{id}/relations", ASSET_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                {"targetId":"%s","relationType":"DEPENDS_ON","description":"Observed dependency","sourceSystem":"AWS_CONFIG","confidence":"0.95"}
                """
                                        .formatted(target.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationType", is("DEPENDS_ON")))
                .andExpect(jsonPath("$.description", is("Observed dependency")))
                .andExpect(jsonPath("$.sourceSystem", is("AWS_CONFIG")))
                .andExpect(jsonPath("$.confidence", is("0.95")));
    }

    @Test
    void createRelationForwardsKnowledgeStateIntoCommand() throws Exception {
        // GC-M018: knowledgeState on a topology edge round-trips through
        // the controller into CreateAssetRelationCommand. ArgumentCaptor
        // anchors the assertion to the actual command shape — without it,
        // a controller drop would still pass because the mock return value
        // is independent of the request body.
        var source = makeAsset();
        var target = makeAsset();
        setField(target, "id", UUID.randomUUID());
        var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.now());
        setField(relation, "updatedAt", Instant.now());
        relation.setKnowledgeState(com.keplerops.groundcontrol.domain.assets.state.KnowledgeState.UNKNOWN);

        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.createRelation(
                        eq(PROJECT_ID),
                        any(com.keplerops.groundcontrol.domain.assets.service.CreateAssetRelationCommand.class),
                        eq(ASSET_ID)))
                .thenReturn(relation);

        mockMvc.perform(post("/api/v1/assets/{id}/relations", ASSET_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                {"targetId":"%s","relationType":"DEPENDS_ON","knowledgeState":"UNKNOWN"}
                """
                                        .formatted(target.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.knowledgeState", is("UNKNOWN")));

        var captor = ArgumentCaptor.forClass(
                com.keplerops.groundcontrol.domain.assets.service.CreateAssetRelationCommand.class);
        verify(assetService).createRelation(eq(PROJECT_ID), captor.capture(), eq(ASSET_ID));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().knowledgeState())
                .isEqualTo(com.keplerops.groundcontrol.domain.assets.state.KnowledgeState.UNKNOWN);
    }
}

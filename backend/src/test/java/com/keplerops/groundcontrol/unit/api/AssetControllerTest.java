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
import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.service.AssetCycleEdge;
import com.keplerops.groundcontrol.domain.assets.service.AssetCycleResult;
import com.keplerops.groundcontrol.domain.assets.service.AssetService;
import com.keplerops.groundcontrol.domain.assets.service.AssetSubgraphResult;
import com.keplerops.groundcontrol.domain.assets.service.AssetTopologyService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void getByIdReturnsAsset() throws Exception {
        when(assetService.getById(ASSET_ID)).thenReturn(makeAsset());

        mockMvc.perform(get("/api/v1/assets/{id}", ASSET_ID))
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
        when(assetService.update(eq(ASSET_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/assets/{id}", ASSET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"name":"Updated Server"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Server")));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/assets/{id}", ASSET_ID)).andExpect(status().isNoContent());

        verify(assetService).delete(ASSET_ID);
    }

    @Test
    void archiveReturnsAsset() throws Exception {
        var archived = makeAsset();
        setField(archived, "archivedAt", Instant.now());
        when(assetService.archive(ASSET_ID)).thenReturn(archived);

        mockMvc.perform(post("/api/v1/assets/{id}/archive", ASSET_ID))
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

        when(assetService.createRelation(eq(ASSET_ID), any(), eq(AssetRelationType.DEPENDS_ON)))
                .thenReturn(relation);

        mockMvc.perform(post("/api/v1/assets/{id}/relations", ASSET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"targetId":"%s","relationType":"DEPENDS_ON"}
                """
                                .formatted(target.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationType", is("DEPENDS_ON")));
    }

    @Test
    void getRelationsReturnsList() throws Exception {
        var source = makeAsset();
        var target = makeAsset();
        setField(target, "id", UUID.randomUUID());

        var relation = new AssetRelation(source, target, AssetRelationType.COMMUNICATES_WITH);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.now());

        when(assetService.getRelations(ASSET_ID)).thenReturn(List.of(relation));

        mockMvc.perform(get("/api/v1/assets/{id}/relations", ASSET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].relationType", is("COMMUNICATES_WITH")));
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
        when(topologyService.impactAnalysis(ASSET_ID)).thenReturn(Set.of(asset));

        mockMvc.perform(get("/api/v1/assets/{id}/topology/impact", ASSET_ID))
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
        var link = new AssetLink(asset, AssetLinkTargetType.REQUIREMENT, "GC-M010", AssetLinkType.IMPLEMENTS);
        setField(link, "id", UUID.randomUUID());
        setField(link, "createdAt", Instant.now());
        setField(link, "updatedAt", Instant.now());
        return link;
    }

    @Test
    void createLinkReturns201() throws Exception {
        when(assetService.createLink(eq(ASSET_ID), any())).thenReturn(makeLink());

        mockMvc.perform(
                        post("/api/v1/assets/{id}/links", ASSET_ID)
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
        when(assetService.getLinksForAsset(ASSET_ID)).thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/assets/{id}/links", ASSET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType", is("REQUIREMENT")));
    }

    @Test
    void getLinksWithTargetTypeFilter() throws Exception {
        when(assetService.getLinksForAssetByTargetType(ASSET_ID, AssetLinkTargetType.REQUIREMENT))
                .thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/assets/{id}/links", ASSET_ID).param("target_type", "REQUIREMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deleteLinkReturns204() throws Exception {
        var linkId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/assets/{id}/links/{linkId}", ASSET_ID, linkId))
                .andExpect(status().isNoContent());

        verify(assetService).deleteLink(ASSET_ID, linkId);
    }

    @Test
    void getLinksByTargetReturnsList() throws Exception {
        when(assetService.getLinksByTarget(AssetLinkTargetType.REQUIREMENT, "GC-M010"))
                .thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/assets/links/by-target")
                        .param("target_type", "REQUIREMENT")
                        .param("target_identifier", "GC-M010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assetUid", is("ASSET-001")));
    }
}

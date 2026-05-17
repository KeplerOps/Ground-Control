package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.assets.AssetSubtypeSchemaController;
import com.keplerops.groundcontrol.domain.assets.model.AssetSubtypeSchema;
import com.keplerops.groundcontrol.domain.assets.service.AssetService;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetSubtypeSchemaCommand;
import com.keplerops.groundcontrol.domain.assets.service.UpdateAssetSubtypeSchemaCommand;
import com.keplerops.groundcontrol.domain.assets.state.AssetSubtypeSchemaStatus;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.Map;
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
@WebMvcTest(AssetSubtypeSchemaController.class)
class AssetSubtypeSchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SCHEMA_ID = UUID.fromString("00000000-0000-0000-0000-000000000077");

    private AssetSubtypeSchema makeSchema() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var schema = new AssetSubtypeSchema(
                project,
                AssetType.WORKLOAD,
                "aws_ec2",
                "v1",
                Map.of("fields", Map.of("region", Map.of("type", "STRING"))));
        setField(schema, "id", SCHEMA_ID);
        setField(schema, "createdAt", Instant.now());
        setField(schema, "updatedAt", Instant.now());
        return schema;
    }

    @Test
    void registerReturns201AndCarriesAllFields() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.registerSubtypeSchema(any())).thenReturn(makeSchema());

        mockMvc.perform(
                        post("/api/v1/assets/subtype-schemas")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "assetType":"WORKLOAD",
                                          "subtype":"aws_ec2",
                                          "schemaVersion":"v1",
                                          "description":"AWS EC2 workload",
                                          "schemaBody":{"fields":{"region":{"type":"STRING"}}}
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetType", is("WORKLOAD")))
                .andExpect(jsonPath("$.subtype", is("aws_ec2")))
                .andExpect(jsonPath("$.schemaVersion", is("v1")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        var captor = ArgumentCaptor.forClass(CreateAssetSubtypeSchemaCommand.class);
        verify(assetService).registerSubtypeSchema(captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.assetType()).isEqualTo(AssetType.WORKLOAD);
        org.assertj.core.api.Assertions.assertThat(cmd.subtype()).isEqualTo("aws_ec2");
        org.assertj.core.api.Assertions.assertThat(cmd.schemaVersion()).isEqualTo("v1");
        org.assertj.core.api.Assertions.assertThat(cmd.description()).isEqualTo("AWS EC2 workload");
        org.assertj.core.api.Assertions.assertThat(cmd.schemaBody()).containsKey("fields");
    }

    @Test
    void registerWithBlankSubtypeFailsValidation() throws Exception {
        // Bean Validation @NotBlank maps through GlobalExceptionHandler to 422
        // (the repo-wide convention for "request shape OK, content invalid").
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/assets/subtype-schemas")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "assetType":"WORKLOAD",
                                          "subtype":"",
                                          "schemaVersion":"v1"
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getByIdReturnsSchema() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getSubtypeSchema(PROJECT_ID, SCHEMA_ID)).thenReturn(makeSchema());

        mockMvc.perform(get("/api/v1/assets/subtype-schemas/{id}", SCHEMA_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtype", is("aws_ec2")));
    }

    @Test
    void getActiveLooksUpByAssetTypeAndSubtype() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.getActiveSubtypeSchema(PROJECT_ID, AssetType.WORKLOAD, "aws_ec2"))
                .thenReturn(makeSchema());

        mockMvc.perform(get("/api/v1/assets/subtype-schemas/active")
                        .param("project", "ground-control")
                        .param("assetType", "WORKLOAD")
                        .param("subtype", "aws_ec2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void listReturnsResults() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.listSubtypeSchemas(PROJECT_ID, null, null)).thenReturn(java.util.List.of(makeSchema()));

        mockMvc.perform(get("/api/v1/assets/subtype-schemas").param("project", "ground-control"))
                .andExpect(status().isOk())
                // Pin cardinality so a regression that returns all schemas
                // across projects (instead of the project-scoped slice)
                // breaks here rather than passing via $[0].subtype only.
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].subtype", is("aws_ec2")));
    }

    @Test
    void updateMapsClearFlagsIntoCommand() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.updateSubtypeSchema(eq(PROJECT_ID), eq(SCHEMA_ID), any()))
                .thenReturn(makeSchema());

        mockMvc.perform(
                        put("/api/v1/assets/subtype-schemas/{id}", SCHEMA_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"description":"Refined","clearSchemaBody":true}
                                        """))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(UpdateAssetSubtypeSchemaCommand.class);
        verify(assetService).updateSubtypeSchema(eq(PROJECT_ID), eq(SCHEMA_ID), captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.description()).isEqualTo("Refined");
        org.assertj.core.api.Assertions.assertThat(cmd.clearSchemaBody()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cmd.clearDescription()).isFalse();
    }

    @Test
    void deprecateReturnsSchemaWithStatusDeprecated() throws Exception {
        var deprecated = makeSchema();
        deprecated.setStatus(AssetSubtypeSchemaStatus.DEPRECATED);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(assetService.deprecateSubtypeSchema(PROJECT_ID, SCHEMA_ID)).thenReturn(deprecated);

        mockMvc.perform(post("/api/v1/assets/subtype-schemas/{id}/deprecate", SCHEMA_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DEPRECATED")));
    }
}

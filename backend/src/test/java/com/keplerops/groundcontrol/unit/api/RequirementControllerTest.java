package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.api.requirements.RequirementController;
import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
import com.keplerops.groundcontrol.domain.exception.AuthorizationException;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.service.CreateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.service.CreateTraceabilityLinkCommand;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementFilter;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementService;
import com.keplerops.groundcontrol.domain.requirements.service.TraceabilityService;
import com.keplerops.groundcontrol.domain.requirements.service.UpdateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RequirementController.class)
class RequirementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RequirementService requirementService;

    @MockitoBean
    private TraceabilityService traceabilityService;

    private static Requirement createRequirement(String uid) {
        var req = new Requirement(uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", UUID.randomUUID());
        setField(req, "createdAt", Instant.now());
        setField(req, "updatedAt", Instant.now());
        return req;
    }

    private static RequirementRelation createRelation(Requirement source, Requirement target) {
        var rel = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
        setField(rel, "id", UUID.randomUUID());
        setField(rel, "createdAt", Instant.now());
        return rel;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class Create {

        @Test
        void returns201WithDraftStatus() throws Exception {
            var req = createRequirement("REQ-001");
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);

            mockMvc.perform(post("/api/v1/requirements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "uid", "REQ-001",
                                    "title", "Test Title",
                                    "statement", "Test Statement",
                                    "requirementType", "FUNCTIONAL",
                                    "priority", "MUST"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.uid", is("REQ-001")))
                    .andExpect(jsonPath("$.status", is("DRAFT")));
        }

        @Test
        void blankTitle_returns422() throws Exception {
            mockMvc.perform(post("/api/v1/requirements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("uid", "REQ-001", "title", "", "statement", "Stmt"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code", is("validation_error")));
        }

        @Test
        void duplicateUid_returns409() throws Exception {
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new ConflictException("Already exists"));

            mockMvc.perform(post("/api/v1/requirements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("uid", "REQ-001", "title", "Title", "statement", "Stmt"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code", is("conflict")));
        }
    }

    @Nested
    class GetById {

        @Test
        void returns200() throws Exception {
            var req = createRequirement("REQ-001");
            when(requirementService.getById(req.getId())).thenReturn(req);

            mockMvc.perform(get("/api/v1/requirements/" + req.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid", is("REQ-001")));
        }

        @Test
        void notFound_returns404() throws Exception {
            var id = UUID.randomUUID();
            when(requirementService.getById(id)).thenThrow(new NotFoundException("Not found"));

            mockMvc.perform(get("/api/v1/requirements/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code", is("not_found")));
        }
    }

    @Nested
    class GetByUid {

        @Test
        void returns200() throws Exception {
            var req = createRequirement("REQ-001");
            when(requirementService.getByUid("REQ-001")).thenReturn(req);

            mockMvc.perform(get("/api/v1/requirements/uid/REQ-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid", is("REQ-001")));
        }
    }

    @Nested
    class ListRequirements {

        @Test
        void returns200WithPagination() throws Exception {
            var req = createRequirement("REQ-001");
            when(requirementService.list(any(Pageable.class), any(RequirementFilter.class)))
                    .thenReturn(new PageImpl<>(List.of(req)));

            mockMvc.perform(get("/api/v1/requirements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].uid", is("REQ-001")));
        }

        @Test
        void returns200WithFilterParams() throws Exception {
            var req = createRequirement("REQ-001");
            when(requirementService.list(any(Pageable.class), any(RequirementFilter.class)))
                    .thenReturn(new PageImpl<>(List.of(req)));

            mockMvc.perform(get("/api/v1/requirements")
                            .param("status", "DRAFT")
                            .param("type", "FUNCTIONAL")
                            .param("wave", "1")
                            .param("search", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].uid", is("REQ-001")));
        }
    }

    @Nested
    class Update {

        @Test
        void returns200() throws Exception {
            var req = createRequirement("REQ-001");
            req.setTitle("Updated");
            when(requirementService.update(eq(req.getId()), any(UpdateRequirementCommand.class)))
                    .thenReturn(req);

            mockMvc.perform(put("/api/v1/requirements/" + req.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("uid", "REQ-001", "title", "Updated", "statement", "Stmt"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Updated")));
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void returns200() throws Exception {
            var req = createRequirement("REQ-001");
            setField(req, "status", Status.ACTIVE);
            when(requirementService.transitionStatus(req.getId(), Status.ACTIVE))
                    .thenReturn(req);

            mockMvc.perform(post("/api/v1/requirements/" + req.getId() + "/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        void invalidTransition_returns422() throws Exception {
            var id = UUID.randomUUID();
            when(requirementService.transitionStatus(id, Status.ARCHIVED))
                    .thenThrow(new DomainValidationException(
                            "Cannot transition",
                            "invalid_status_transition",
                            Map.of("current_status", "DRAFT", "target_status", "ARCHIVED")));

            mockMvc.perform(post("/api/v1/requirements/" + id + "/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"ARCHIVED\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code", is("invalid_status_transition")));
        }
    }

    @Nested
    class Archive {

        @Test
        void returns200() throws Exception {
            var req = createRequirement("REQ-001");
            setField(req, "status", Status.ARCHIVED);
            setField(req, "archivedAt", Instant.now());
            when(requirementService.archive(req.getId())).thenReturn(req);

            mockMvc.perform(post("/api/v1/requirements/" + req.getId() + "/archive"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ARCHIVED")))
                    .andExpect(jsonPath("$.archivedAt", notNullValue()));
        }
    }

    @Nested
    class Relations {

        @Test
        void createRelation_returns201() throws Exception {
            var source = createRequirement("REQ-001");
            var target = createRequirement("REQ-002");
            var rel = createRelation(source, target);
            when(requirementService.createRelation(source.getId(), target.getId(), RelationType.DEPENDS_ON))
                    .thenReturn(rel);

            mockMvc.perform(post("/api/v1/requirements/" + source.getId() + "/relations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("targetId", target.getId(), "relationType", "DEPENDS_ON"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.relationType", is("DEPENDS_ON")))
                    .andExpect(jsonPath("$.sourceUid", is("REQ-001")))
                    .andExpect(jsonPath("$.targetUid", is("REQ-002")));
        }

        @Test
        void getRelations_returns200() throws Exception {
            var source = createRequirement("REQ-001");
            var target = createRequirement("REQ-002");
            var rel = createRelation(source, target);
            when(requirementService.getRelations(source.getId())).thenReturn(List.of(rel));

            mockMvc.perform(get("/api/v1/requirements/" + source.getId() + "/relations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].relationType", is("DEPENDS_ON")));
        }
    }

    @Nested
    class DeleteRelation {

        @Test
        void returns204() throws Exception {
            var reqId = UUID.randomUUID();
            var relationId = UUID.randomUUID();
            doNothing().when(requirementService).deleteRelation(reqId, relationId);

            mockMvc.perform(delete("/api/v1/requirements/" + reqId + "/relations/" + relationId))
                    .andExpect(status().isNoContent());
        }

        @Test
        void notFound_returns404() throws Exception {
            var reqId = UUID.randomUUID();
            var relationId = UUID.randomUUID();
            doThrow(new NotFoundException("Not found")).when(requirementService).deleteRelation(reqId, relationId);

            mockMvc.perform(delete("/api/v1/requirements/" + reqId + "/relations/" + relationId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Traceability {

        private static TraceabilityLink createLink(Requirement req) {
            var link = new TraceabilityLink(req, ArtifactType.GITHUB_ISSUE, "GH-123", LinkType.IMPLEMENTS);
            setField(link, "id", UUID.randomUUID());
            setField(link, "createdAt", Instant.now());
            setField(link, "updatedAt", Instant.now());
            return link;
        }

        @Test
        void getLinks_returns200() throws Exception {
            var req = createRequirement("REQ-001");
            var link = createLink(req);
            when(traceabilityService.getLinksForRequirement(req.getId())).thenReturn(List.of(link));

            mockMvc.perform(get("/api/v1/requirements/" + req.getId() + "/traceability"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].artifactType", is("GITHUB_ISSUE")))
                    .andExpect(jsonPath("$[0].artifactIdentifier", is("GH-123")))
                    .andExpect(jsonPath("$[0].linkType", is("IMPLEMENTS")));
        }

        @Test
        void createLink_returns201() throws Exception {
            var req = createRequirement("REQ-001");
            var link = createLink(req);
            when(traceabilityService.createLink(eq(req.getId()), any(CreateTraceabilityLinkCommand.class)))
                    .thenReturn(link);

            mockMvc.perform(post("/api/v1/requirements/" + req.getId() + "/traceability")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "artifactType", "GITHUB_ISSUE",
                                    "artifactIdentifier", "GH-123",
                                    "linkType", "IMPLEMENTS"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.artifactType", is("GITHUB_ISSUE")));
        }

        @Test
        void deleteLink_returns204() throws Exception {
            var reqId = UUID.randomUUID();
            var linkId = UUID.randomUUID();
            doNothing().when(traceabilityService).deleteLink(linkId);

            mockMvc.perform(delete("/api/v1/requirements/" + reqId + "/traceability/" + linkId))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class ExceptionHandlerCoverage {

        @Test
        void authenticationException_returns401() throws Exception {
            var id = UUID.randomUUID();
            when(requirementService.getById(id)).thenThrow(new AuthenticationException("unauthenticated"));

            mockMvc.perform(get("/api/v1/requirements/" + id))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code", is("authentication_error")));
        }

        @Test
        void authorizationException_returns403() throws Exception {
            var id = UUID.randomUUID();
            when(requirementService.getById(id)).thenThrow(new AuthorizationException("forbidden"));

            mockMvc.perform(get("/api/v1/requirements/" + id))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code", is("authorization_error")));
        }

        @Test
        void unhandledGroundControlException_returns500() throws Exception {
            var id = UUID.randomUUID();
            when(requirementService.getById(id)).thenThrow(new GroundControlException("unexpected", "internal_error"));

            mockMvc.perform(get("/api/v1/requirements/" + id))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error.code", is("internal_error")));
        }
    }
}

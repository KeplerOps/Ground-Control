package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.TestUtil;
import com.keplerops.groundcontrol.api.projects.ProjectController;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.CreateProjectCommand;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.projects.service.UpdateProjectCommand;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    private Project makeProject(String identifier, String name) {
        var project = new Project(identifier, name);
        TestUtil.setField(project, "id", UUID.randomUUID());
        return project;
    }

    @Nested
    class Create {

        @Test
        void returns201() throws Exception {
            var project = makeProject("my-project", "My Project");
            when(projectService.create(any(CreateProjectCommand.class))).thenReturn(project);

            mockMvc.perform(
                            post("/api/v1/projects")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                            {
                              "identifier": "my-project",
                              "name": "My Project"
                            }
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.identifier", is("my-project")))
                    .andExpect(jsonPath("$.name", is("My Project")));
        }

        @Test
        void blankIdentifier_returns422() throws Exception {
            mockMvc.perform(
                            post("/api/v1/projects")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                            {
                              "identifier": "",
                              "name": "My Project"
                            }
                            """))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void invalidIdentifierPattern_returns422() throws Exception {
            mockMvc.perform(
                            post("/api/v1/projects")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                            {
                              "identifier": "My Project!",
                              "name": "My Project"
                            }
                            """))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void duplicateIdentifier_returns409() throws Exception {
            when(projectService.create(any(CreateProjectCommand.class)))
                    .thenThrow(new ConflictException("Project with identifier 'my-project' already exists"));

            mockMvc.perform(
                            post("/api/v1/projects")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                            {
                              "identifier": "my-project",
                              "name": "My Project"
                            }
                            """))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class ListProjects {

        @Test
        void returns200() throws Exception {
            when(projectService.list()).thenReturn(List.of(makeProject("p1", "P1"), makeProject("p2", "P2")));

            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].identifier", is("p1")));
        }
    }

    @Nested
    class GetByIdentifier {

        @Test
        void returns200() throws Exception {
            when(projectService.getByIdentifier("my-project")).thenReturn(makeProject("my-project", "My Project"));

            mockMvc.perform(get("/api/v1/projects/my-project"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.identifier", is("my-project")));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(projectService.getByIdentifier("nonexistent"))
                    .thenThrow(new NotFoundException("Project not found: nonexistent"));

            mockMvc.perform(get("/api/v1/projects/nonexistent")).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Update {

        @Test
        void returns200() throws Exception {
            var updated = makeProject("my-project", "Updated Name");
            when(projectService.updateByIdentifier(eq("my-project"), any(UpdateProjectCommand.class)))
                    .thenReturn(updated);

            mockMvc.perform(
                            put("/api/v1/projects/my-project")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                            {
                              "name": "Updated Name"
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Name")));
        }
    }
}

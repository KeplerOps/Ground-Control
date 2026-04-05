package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GraphProjectionRegistryService {

    private final ProjectRepository projectRepository;
    private final List<GraphProjectionContributor> contributors;

    public GraphProjectionRegistryService(
            ProjectRepository projectRepository, List<GraphProjectionContributor> contributors) {
        this.projectRepository = projectRepository;
        this.contributors = contributors;
    }

    public GraphProjection buildProjection() {
        var nodes = new ArrayList<com.keplerops.groundcontrol.domain.graph.model.GraphNode>();
        var edges = new ArrayList<com.keplerops.groundcontrol.domain.graph.model.GraphEdge>();
        for (var project : projectRepository.findAll()) {
            for (var contributor : contributors) {
                nodes.addAll(contributor.contributeNodes(project.getId()));
                edges.addAll(contributor.contributeEdges(project.getId()));
            }
        }
        return new GraphProjection(nodes, edges);
    }
}

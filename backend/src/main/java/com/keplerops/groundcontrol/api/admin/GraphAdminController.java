package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphAdminController {

    private final GraphClient graphClient;

    public GraphAdminController(GraphClient graphClient) {
        this.graphClient = graphClient;
    }

    @PostMapping("/api/v1/admin/graph/materialize")
    public void materializeGraph() {
        graphClient.materializeGraph();
    }
}

package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: split admin/read endpoints into separate controllers
@RestController
public class GraphController {

    private final GraphClient graphClient;

    public GraphController(GraphClient graphClient) {
        this.graphClient = graphClient;
    }

    @PostMapping("/api/v1/admin/graph/materialize")
    public void materializeGraph() {
        graphClient.materializeGraph();
    }

    @GetMapping("/api/v1/graph/ancestors/{uid}")
    public List<String> getAncestors(@PathVariable String uid, @RequestParam(defaultValue = "10") int depth) {
        return graphClient.getAncestors(uid, depth);
    }

    @GetMapping("/api/v1/graph/descendants/{uid}")
    public List<String> getDescendants(@PathVariable String uid, @RequestParam(defaultValue = "10") int depth) {
        return graphClient.getDescendants(uid, depth);
    }

    @GetMapping("/api/v1/graph/paths")
    public List<List<String>> findPaths(@RequestParam String source, @RequestParam String target) {
        return graphClient.findPaths(source, target);
    }
}

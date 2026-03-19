package com.keplerops.groundcontrol.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards non-API, non-static routes to {@code index.html} so that React Router can handle
 * client-side routing. The regex excludes paths starting with "api" or "actuator" and paths
 * containing a dot (static file requests like .js, .css, .html).
 */
@Controller
public class SpaController {

    @GetMapping("/{path:^(?!api|actuator)[^\\.]*$}")
    public String forward() {
        return "forward:/index.html";
    }
}

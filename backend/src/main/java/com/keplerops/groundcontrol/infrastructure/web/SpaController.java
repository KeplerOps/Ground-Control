package com.keplerops.groundcontrol.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards non-API, non-static routes to {@code index.html} so that React Router can handle
 * client-side routing.
 *
 * <p>Spring's {@code @GetMapping} path variables match a single segment only, so we need explicit
 * patterns for each depth level used by the React Router routes (up to 4 segments:
 * {@code /p/:projectId/requirements/:id}).
 *
 * <p>The regex on each segment excludes paths starting with "api" or "actuator" and paths
 * containing a dot (static file requests like .js, .css, .html).
 */
@Controller
public class SpaController {

    private static final String NO_DOT = "^(?!api|actuator)[^\\.]*$";

    @GetMapping({
        "/{p1:" + NO_DOT + "}",
        "/{p1:" + NO_DOT + "}/{p2:" + NO_DOT + "}",
        "/{p1:" + NO_DOT + "}/{p2:" + NO_DOT + "}/{p3:" + NO_DOT + "}",
        "/{p1:" + NO_DOT + "}/{p2:" + NO_DOT + "}/{p3:" + NO_DOT + "}/{p4:" + NO_DOT + "}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

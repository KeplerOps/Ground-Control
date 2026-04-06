package com.keplerops.groundcontrol.infrastructure.web;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Forwards non-API, non-static routes to {@code index.html} so that React Router can handle
 * client-side routing.
 *
 * <p>Spring's {@code @GetMapping} path variables match a single segment only, so we need explicit
 * patterns for each depth level. We support up to 6 segments to provide headroom beyond the
 * current deepest route ({@code /p/:projectId/requirements/:id} = 4 segments).
 *
 * <p>The regex on each segment excludes paths starting with "api" or "actuator" and paths
 * containing a dot (static file requests like .js, .css, .html).
 */
@Controller
public class SpaController {

    private static final String NO_DOT = "^(?!api|actuator)[^\\.]*$";
    private static final String S1 = "/{a:" + NO_DOT + "}";
    private static final String S2 = S1 + "/{b:" + NO_DOT + "}";
    private static final String S3 = S2 + "/{c:" + NO_DOT + "}";
    private static final String S4 = S3 + "/{d:" + NO_DOT + "}";
    private static final String S5 = S4 + "/{e:" + NO_DOT + "}";
    private static final String S6 = S5 + "/{f:" + NO_DOT + "}";

    @GetMapping({S1, S2, S3, S4, S5, S6})
    public String forward(@PathVariable Map<String, String> segments) {
        return "forward:/index.html";
    }
}

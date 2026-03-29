package com.keplerops.groundcontrol.domain.documents.service;

import java.util.Map;
import org.springframework.stereotype.Service;

/** Serializes a document's reading order to a self-contained HTML page. */
@Service
public class DocumentExportHtmlService {

    private static final String CSS =
            """
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                   max-width: 960px; margin: 0 auto; padding: 2rem; color: #1a1a1a; line-height: 1.6; }
            h1 { border-bottom: 2px solid #333; padding-bottom: 0.5rem; }
            section { margin-bottom: 2rem; }
            .text-block { margin: 1rem 0; padding: 0.75rem 1rem; background: #f8f9fa;
                          border-left: 3px solid #6c757d; }
            article.requirement { margin: 1.5rem 0; padding: 1rem; border: 1px solid #dee2e6;
                                  border-radius: 4px; }
            .req-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
            .uid-badge { background: #e9ecef; padding: 0.15rem 0.5rem; border-radius: 3px;
                         font-family: monospace; font-size: 0.85rem; font-weight: 600; }
            .req-title { font-weight: 600; font-size: 1.1rem; }
            .statement { margin: 0.75rem 0; white-space: pre-wrap; }
            .comment { margin: 0.75rem 0; font-size: 0.9rem; color: #495057; font-style: italic; }
            .relations { margin-top: 0.5rem; font-size: 0.9rem; color: #495057; }
            .relations span { font-family: monospace; }
            .meta { color: #6c757d; font-size: 0.85rem; margin-top: 1rem; }
            @media print { body { max-width: 100%; } article.requirement { break-inside: avoid; } }
            """;

    public String toHtml(DocumentReadingOrder order, Map<String, RequirementExportData> requirementsByUid) {
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>").append(escape(order.title())).append("</title>\n");
        sb.append("<style>\n").append(CSS).append("</style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("<h1>").append(escape(order.title())).append("</h1>\n");

        if (order.version() != null && !order.version().isEmpty()) {
            sb.append("<p class=\"meta\">Version: ")
                    .append(escape(order.version()))
                    .append("</p>\n");
        }
        if (order.description() != null && !order.description().isEmpty()) {
            sb.append("<p>").append(escape(order.description())).append("</p>\n");
        }

        for (var section : order.sections()) {
            writeSection(sb, section, requirementsByUid, 2);
        }

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private void writeSection(
            StringBuilder sb,
            ReadingOrderNode section,
            Map<String, RequirementExportData> requirementsByUid,
            int headingLevel) {
        int level = Math.min(headingLevel, 6);
        sb.append("<section>\n");
        sb.append("<h")
                .append(level)
                .append('>')
                .append(escape(section.title()))
                .append("</h")
                .append(level)
                .append(">\n");

        for (var item : section.content()) {
            if ("REQUIREMENT".equals(item.contentType()) && item.requirementUid() != null) {
                var req = requirementsByUid.get(item.requirementUid());
                if (req != null) {
                    writeRequirement(sb, req);
                }
            } else if ("TEXT_BLOCK".equals(item.contentType()) && item.textContent() != null) {
                sb.append("<div class=\"text-block\"><p>")
                        .append(escape(item.textContent()))
                        .append("</p></div>\n");
            }
        }

        for (var child : section.children()) {
            writeSection(sb, child, requirementsByUid, headingLevel + 1);
        }

        sb.append("</section>\n");
    }

    private void writeRequirement(StringBuilder sb, RequirementExportData req) {
        sb.append("<article class=\"requirement\">\n");
        sb.append("<div class=\"req-header\">");
        sb.append("<span class=\"uid-badge\">").append(escape(req.uid())).append("</span>");
        sb.append("<span class=\"req-title\">").append(escape(req.title())).append("</span>");
        sb.append("</div>\n");
        sb.append("<div class=\"statement\">").append(escape(req.statement())).append("</div>\n");

        if (req.comment() != null && !req.comment().isEmpty()) {
            sb.append("<div class=\"comment\"><strong>Comment:</strong> ")
                    .append(escape(req.comment()))
                    .append("</div>\n");
        }

        if (!req.parentUids().isEmpty()) {
            sb.append("<div class=\"relations\">Parent: ");
            for (int i = 0; i < req.parentUids().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("<span>").append(escape(req.parentUids().get(i))).append("</span>");
            }
            sb.append("</div>\n");
        }

        sb.append("</article>\n");
    }

    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

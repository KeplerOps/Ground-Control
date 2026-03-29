package com.keplerops.groundcontrol.domain.documents.service;

import java.util.Map;
import org.springframework.stereotype.Service;

/** Serializes a document's reading order to StrictDoc (.sdoc) format. */
@Service
public class DocumentExportSdocService {

    public String toSdoc(DocumentReadingOrder order, Map<String, RequirementExportData> requirementsByUid) {
        var sb = new StringBuilder();
        for (var section : order.sections()) {
            writeSection(sb, section, requirementsByUid);
        }
        return sb.toString();
    }

    private void writeSection(
            StringBuilder sb, ReadingOrderNode section, Map<String, RequirementExportData> requirementsByUid) {
        sb.append("[[SECTION]]\n");
        sb.append("TITLE: ").append(section.title()).append("\n\n");

        for (var item : section.content()) {
            if ("REQUIREMENT".equals(item.contentType()) && item.requirementUid() != null) {
                var req = requirementsByUid.get(item.requirementUid());
                if (req != null) {
                    writeRequirement(sb, req);
                }
            } else if ("TEXT_BLOCK".equals(item.contentType()) && item.textContent() != null) {
                writeTextBlock(sb, item.textContent());
            }
        }

        for (var child : section.children()) {
            writeSection(sb, child, requirementsByUid);
        }

        sb.append("[[/SECTION]]\n\n");
    }

    private void writeRequirement(StringBuilder sb, RequirementExportData req) {
        sb.append("[REQUIREMENT]\n");
        sb.append("UID: ").append(req.uid()).append('\n');
        sb.append("TITLE: ").append(req.title()).append('\n');
        sb.append("STATEMENT: >>>\n").append(req.statement()).append("\n<<<\n");

        if (req.comment() != null && !req.comment().isEmpty()) {
            sb.append("COMMENT: ").append(req.comment()).append('\n');
        }

        if (!req.parentUids().isEmpty()) {
            sb.append("RELATIONS:\n");
            for (String parentUid : req.parentUids()) {
                sb.append("- TYPE: Parent\n  VALUE: ").append(parentUid).append('\n');
            }
        }

        sb.append('\n');
    }

    private void writeTextBlock(StringBuilder sb, String text) {
        sb.append("[TEXT]\n");
        sb.append(text).append("\n\n");
    }
}

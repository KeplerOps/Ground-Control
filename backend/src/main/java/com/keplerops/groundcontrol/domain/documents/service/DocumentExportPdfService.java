package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.requirements.service.ExportException;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Serializes a document's reading order to PDF format for formal distribution. */
@Service
public class DocumentExportPdfService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font SUBSECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font UID_FONT = FontFactory.getFont(FontFactory.COURIER_BOLD, 9);
    private static final Font REQ_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font RELATION_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(230, 230, 230);
    private static final Color TEXT_BLOCK_BG = new Color(248, 249, 250);

    public byte[] toPdf(DocumentReadingOrder order, Map<String, RequirementExportData> requirementsByUid) {
        var out = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addDocumentHeader(document, order);
            for (var section : order.sections()) {
                addSection(document, section, requirementsByUid, 0);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ExportException("Failed to generate document PDF export", e);
        }
    }

    private void addDocumentHeader(Document document, DocumentReadingOrder order) {
        var title = new Paragraph(nullSafe(order.title()), TITLE_FONT);
        title.setSpacingAfter(8f);
        document.add(title);

        if (order.version() != null && !order.version().isEmpty()) {
            var version = new Paragraph("Version " + order.version(), SUBTITLE_FONT);
            version.setSpacingAfter(4f);
            document.add(version);
        }
        if (order.description() != null && !order.description().isEmpty()) {
            var desc = new Paragraph(order.description(), BODY_FONT);
            desc.setSpacingAfter(20f);
            document.add(desc);
        }
    }

    private void addSection(
            Document document,
            ReadingOrderNode section,
            Map<String, RequirementExportData> requirementsByUid,
            int depth) {
        Font headingFont = depth == 0 ? SECTION_FONT : SUBSECTION_FONT;
        var heading = new Paragraph(nullSafe(section.title()), headingFont);
        heading.setSpacingBefore(depth == 0 ? 20f : 12f);
        heading.setSpacingAfter(8f);
        document.add(heading);

        for (var item : section.content()) {
            if ("REQUIREMENT".equals(item.contentType()) && item.requirementUid() != null) {
                var req = requirementsByUid.get(item.requirementUid());
                if (req != null) {
                    addRequirement(document, req);
                }
            } else if ("TEXT_BLOCK".equals(item.contentType()) && item.textContent() != null) {
                addTextBlock(document, item.textContent());
            }
        }

        for (var child : section.children()) {
            addSection(document, child, requirementsByUid, depth + 1);
        }
    }

    private void addRequirement(Document document, RequirementExportData req) {
        var table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        // Header cell with UID and title
        var headerCell = new PdfPCell();
        headerCell.setBackgroundColor(HEADER_BG);
        headerCell.setPadding(6f);
        var headerPhrase = new Phrase();
        headerPhrase.add(new Phrase(req.uid() + "  ", UID_FONT));
        headerPhrase.add(new Phrase(nullSafe(req.title()), REQ_TITLE_FONT));
        headerCell.setPhrase(headerPhrase);
        table.addCell(headerCell);

        // Statement cell
        var stmtCell = new PdfPCell(new Phrase(nullSafe(req.statement()), BODY_FONT));
        stmtCell.setPadding(6f);
        stmtCell.setBorderWidthTop(0);
        table.addCell(stmtCell);

        // Relations cell (if any)
        if (!req.parentUids().isEmpty()) {
            var relText = "Parent: " + String.join(", ", req.parentUids());
            var relCell = new PdfPCell(new Phrase(relText, RELATION_FONT));
            relCell.setPadding(4f);
            relCell.setBorderWidthTop(0);
            relCell.setBackgroundColor(HEADER_BG);
            table.addCell(relCell);
        }

        document.add(table);
    }

    private void addTextBlock(Document document, String text) {
        var table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(4f);

        var cell = new PdfPCell(new Phrase(nullSafe(text), BODY_FONT));
        cell.setBackgroundColor(TEXT_BLOCK_BG);
        cell.setPadding(8f);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);

        document.add(table);
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}

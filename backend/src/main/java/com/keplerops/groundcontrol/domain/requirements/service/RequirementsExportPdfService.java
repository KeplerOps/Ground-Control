package com.keplerops.groundcontrol.domain.requirements.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class RequirementsExportPdfService {

    private static final String[] TABLE_HEADERS = {"UID", "Title", "Type", "Priority", "Status", "Wave", "Statement"};
    private static final float[] COLUMN_WIDTHS = {8f, 15f, 10f, 8f, 8f, 6f, 45f};

    public byte[] toPdf(RequirementsExportData data) {
        var out = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document, data);
            addRequirementsTable(document, data);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ExportException("Failed to generate PDF export", e);
        }
    }

    private void addTitle(Document document, RequirementsExportData data) {
        var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        var subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        var title = new Paragraph("Requirements Report - " + data.projectIdentifier(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        var subtitle = new Paragraph(
                "Generated: "
                        + data.timestamp()
                                .atZone(java.time.ZoneOffset.UTC)
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        + " | Total: " + data.requirements().size() + " requirements",
                subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20f);
        document.add(subtitle);
    }

    private void addRequirementsTable(Document document, RequirementsExportData data) {
        var table = new PdfPTable(TABLE_HEADERS.length);
        table.setWidthPercentage(100);
        try {
            table.setWidths(COLUMN_WIDTHS);
        } catch (Exception e) {
            throw new ExportException("Failed to set PDF table widths", e);
        }

        var headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        var cellFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

        for (String header : TABLE_HEADERS) {
            var cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4f);
            table.addCell(cell);
        }

        for (var req : data.requirements()) {
            addCell(table, nullSafe(req.uid()), cellFont);
            addCell(table, nullSafe(req.title()), cellFont);
            addCell(table, nullSafe(req.requirementType()), cellFont);
            addCell(table, nullSafe(req.priority()), cellFont);
            addCell(table, nullSafe(req.status()), cellFont);
            addCell(table, req.wave() != null ? req.wave().toString() : "", cellFont);
            addCell(table, nullSafe(req.statement()), cellFont);
        }

        document.add(table);
    }

    private void addCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        var cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}

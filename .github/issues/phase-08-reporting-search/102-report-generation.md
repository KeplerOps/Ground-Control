---
title: "Implement report generation engine (PDF/PPTX/Excel)"
labels: [backend, reporting]
phase: 8
priority: P1
---

## Description

Build the report generation engine for producing executive reports in PDF, PPTX, and Excel formats.

## References

- PRD: Section 4.6 (Reporting & Dashboards — board reports, custom reports)
- User Stories: US-6.1, US-6.2

## Acceptance Criteria

- [ ] Report generation API: `POST /api/v1/reports/generate`
  - Input: template_id, parameters (scope, date range, framework, format)
  - Output: report_id (async generation)
  - `GET /api/v1/reports/{id}` — status + download link when complete
- [ ] Format support: PDF (via WeasyPrint or ReportLab), PPTX (python-pptx), Excel (openpyxl)
- [ ] Standard report types:
  - Risk posture summary (heat map, top risks, trends)
  - Control health report (effectiveness, coverage)
  - Assessment status report (progress, findings)
  - Finding summary report
- [ ] Report templates (Jinja2 HTML → PDF)
- [ ] Charts/visualizations embedded in reports
- [ ] Reports stored as artifacts for download
- [ ] Background job processing for generation
- [ ] Unit tests

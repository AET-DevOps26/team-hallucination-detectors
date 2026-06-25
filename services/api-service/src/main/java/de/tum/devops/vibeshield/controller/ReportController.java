package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.report.HtmlReportRenderer;
import de.tum.devops.vibeshield.report.PdfReportRenderer;
import de.tum.devops.vibeshield.report.ReportData;
import de.tum.devops.vibeshield.security.CurrentUser;
import de.tum.devops.vibeshield.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** Export endpoints for scan reporting (Epic 7). */
@RestController
public class ReportController {

    private final ReportService reportService;
    private final CurrentUser currentUser;
    private final HtmlReportRenderer htmlRenderer = new HtmlReportRenderer();
    private final PdfReportRenderer pdfRenderer = new PdfReportRenderer();

    public ReportController(ReportService reportService, CurrentUser currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/scans/{scanId}/report/data")
    public ResponseEntity<ReportData> reportData(@PathVariable Long scanId) {
        return ResponseEntity.ok(reportService.buildReport(currentUser.require(), scanId));
    }

    @GetMapping(value = "/api/v1/scans/{scanId}/report/summary.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> summaryHtml(@PathVariable Long scanId) {
        ReportData report = reportService.buildReport(currentUser.require(), scanId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(report, "summary", "html"))
                .body(htmlRenderer.renderSummary(report));
    }

    @GetMapping(value = "/api/v1/scans/{scanId}/report/summary.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> summaryPdf(@PathVariable Long scanId) {
        ReportData report = reportService.buildReport(currentUser.require(), scanId);
        return pdfResponse(pdfRenderer.renderSummary(report), report, "summary");
    }

    @GetMapping(value = "/api/v1/scans/{scanId}/report/full.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> fullPdf(@PathVariable Long scanId) {
        ReportData report = reportService.buildReport(currentUser.require(), scanId);
        return pdfResponse(pdfRenderer.renderFull(report), report, "full");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] body, ReportData report, String type) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(report, type, "pdf"))
                .contentLength(body.length)
                .body(body);
    }

    private String attachment(ReportData report, String type, String extension) {
        String filename = "vibeshield-scan-" + report.scanId() + "-" + type + "." + extension;
        return ContentDisposition.attachment().filename(filename).build().toString();
    }
}

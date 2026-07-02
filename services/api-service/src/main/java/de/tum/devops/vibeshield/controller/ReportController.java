package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.generated.api.ReportsApi;
import de.tum.devops.vibeshield.mapper.ReportMapper;
import de.tum.devops.vibeshield.report.HtmlReportRenderer;
import de.tum.devops.vibeshield.report.PdfReportRenderer;
import de.tum.devops.vibeshield.security.CurrentUser;
import de.tum.devops.vibeshield.service.ReportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Implements the generated Reports contract (api/openapi.yaml): computed report data plus
 * downloadable HTML/PDF exports for scan reporting (Epic 7).
 */
@RestController
public class ReportController implements ReportsApi {

    private final ReportService reportService;
    private final CurrentUser currentUser;
    private final HtmlReportRenderer htmlRenderer = new HtmlReportRenderer();
    private final PdfReportRenderer pdfRenderer = new PdfReportRenderer();

    public ReportController(ReportService reportService, CurrentUser currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @Override
    public ResponseEntity<de.tum.devops.vibeshield.generated.model.ReportData> getScanReportData(Long scanId) {
        return ResponseEntity.ok(ReportMapper.toModel(reportService.buildReport(currentUser.require(), scanId)));
    }

    @Override
    public ResponseEntity<String> exportExecutiveSummaryHtml(Long scanId) {
        var report = reportService.buildReport(currentUser.require(), scanId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(report, "summary", "html"))
                .body(htmlRenderer.renderSummary(report));
    }

    @Override
    public ResponseEntity<Resource> exportExecutiveSummaryPdf(Long scanId) {
        var report = reportService.buildReport(currentUser.require(), scanId);
        return pdfResponse(pdfRenderer.renderSummary(report), report, "summary");
    }

    @Override
    public ResponseEntity<Resource> exportFullScanReportPdf(Long scanId) {
        var report = reportService.buildReport(currentUser.require(), scanId);
        return pdfResponse(pdfRenderer.renderFull(report), report, "full");
    }

    private ResponseEntity<Resource> pdfResponse(byte[] body, de.tum.devops.vibeshield.report.ReportData report,
                                                  String type) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(report, type, "pdf"))
                .contentLength(body.length)
                .body(new ByteArrayResource(body));
    }

    private String attachment(de.tum.devops.vibeshield.report.ReportData report, String type, String extension) {
        String filename = "vibeshield-scan-" + report.scanId() + "-" + type + "." + extension;
        return ContentDisposition.attachment().filename(filename).build().toString();
    }
}

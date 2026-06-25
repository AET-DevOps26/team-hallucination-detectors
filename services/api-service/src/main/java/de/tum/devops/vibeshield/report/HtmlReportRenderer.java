package de.tum.devops.vibeshield.report;

import de.tum.devops.vibeshield.generated.model.FindingStatus;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Server-side HTML renderer used directly for HTML export and as the PDF content source. */
public class HtmlReportRenderer {

    public String renderSummary(ReportData report) {
        return render(report, false);
    }

    public String renderFull(ReportData report) {
        return render(report, true);
    }

    private String render(ReportData report, boolean full) {
        String title = full ? "Full Scan Report" : "Executive Security Summary";
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>")
                .append(escape(title))
                .append("</title><style>")
                .append("body{font-family:Arial,sans-serif;color:#18181b;margin:40px;line-height:1.45}")
                .append("h1,h2{margin-bottom:8px} .muted{color:#52525b}.badge{display:inline-block;padding:3px 8px;border-radius:4px;background:#f4f4f5}")
                .append("table{border-collapse:collapse;width:100%;margin-top:12px}th,td{border:1px solid #d4d4d8;padding:8px;text-align:left;vertical-align:top}")
                .append("th{background:#f4f4f5}.pass{color:#047857;font-weight:700}.fail{color:#b91c1c;font-weight:700}")
                .append("</style></head><body>");
        html.append("<h1>").append(escape(title)).append("</h1>");
        html.append("<p class=\"muted\">").append(escape(report.site().name()))
                .append(" · ").append(escape(report.site().url())).append("</p>");
        html.append("<p>Generated ").append(format(report.generatedAt())).append("</p>");
        html.append("<p><strong>Risk level:</strong> ").append(escape(report.executiveSummary().riskLevel()))
                .append(" · <strong>Safe to launch:</strong> ")
                .append(escape(report.safeToLaunch().status())).append("</p>");

        appendChecklist(html, report.safeToLaunch().items());
        appendNextSteps(html, report.executiveSummary().recommendedNextSteps());
        appendFindings(html, full ? report.findings() : report.findings().stream()
                .filter(finding -> finding.status() == FindingStatus.OPEN)
                .limit(8)
                .toList(), full);
        html.append("</body></html>");
        return html.toString();
    }

    private void appendChecklist(StringBuilder html, List<LaunchChecklistItem> items) {
        html.append("<h2>Safe to Launch Checklist</h2><table><thead><tr>")
                .append("<th>What was checked</th><th>Result</th><th>Details</th></tr></thead><tbody>");
        for (LaunchChecklistItem item : items) {
            html.append("<tr><td>").append(escape(item.label())).append("</td><td class=\"")
                    .append("Pass".equals(item.result()) ? "pass" : "Needs attention".equals(item.result()) ? "fail" : "")
                    .append("\">")
                    .append(escape(item.result()))
                    .append("</td><td>").append(escape(item.reason())).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendNextSteps(StringBuilder html, List<ReportNextStep> nextSteps) {
        html.append("<h2>Recommended Next Steps</h2>");
        if (nextSteps.isEmpty()) {
            html.append("<p>No open findings require action right now.</p>");
            return;
        }
        html.append("<table><thead><tr><th>Order</th><th>Action</th><th>Severity</th><th>Effort</th></tr></thead><tbody>");
        for (ReportNextStep step : nextSteps) {
            html.append("<tr><td>").append(step.order()).append("</td><td>")
                    .append(escape(step.title())).append("<br><span class=\"muted\">")
                    .append(escape(step.action())).append("</span></td><td>")
                    .append(escape(step.severity().getValue())).append("</td><td>")
                    .append(escape(step.effort().level())).append(" · ")
                    .append(escape(step.effort().estimate())).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendFindings(StringBuilder html, List<ReportFinding> findings, boolean full) {
        html.append("<h2>").append(full ? "All Findings" : "Top Open Findings").append("</h2>");
        if (findings.isEmpty()) {
            html.append("<p>No findings to show.</p>");
            return;
        }
        html.append("<table><thead><tr><th>Order</th><th>Severity</th><th>Finding</th><th>Effort</th>");
        if (full) {
            html.append("<th>Suggested Fix</th>");
        }
        html.append("</tr></thead><tbody>");
        for (ReportFinding finding : findings) {
            html.append("<tr><td>").append(finding.suggestedFixOrder() == null ? "-" : finding.suggestedFixOrder())
                    .append("</td><td>").append(escape(finding.severity().getValue()))
                    .append("</td><td><strong>").append(escape(finding.title())).append("</strong><br>")
                    .append("<span class=\"muted\">").append(escape(finding.affected())).append("</span><br>")
                    .append(escape(finding.explanation())).append("</td><td>")
                    .append(escape(finding.effort().level())).append(" · ")
                    .append(escape(finding.effort().estimate())).append("</td>");
            if (full) {
                html.append("<td>").append(escape(finding.suggestedFix())).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private String format(java.time.OffsetDateTime dateTime) {
        return dateTime == null ? "n/a" : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

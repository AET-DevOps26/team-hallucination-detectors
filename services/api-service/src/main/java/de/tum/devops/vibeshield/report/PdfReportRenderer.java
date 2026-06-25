package de.tum.devops.vibeshield.report;

import de.tum.devops.vibeshield.generated.model.FindingStatus;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Minimal structured PDF renderer for downloadable reports without a native/browser dependency. */
public class PdfReportRenderer {

    private static final int PAGE_WIDTH = 612;
    private static final int PAGE_HEIGHT = 792;
    private static final int LEFT = 54;
    private static final int TOP = 730;
    private static final int BOTTOM = 58;
    private static final int LINE_HEIGHT = 13;
    private static final int BODY_WRAP = 92;
    private static final int INDENT_WRAP = 84;

    public byte[] renderSummary(ReportData report) {
        List<TextBlock> blocks = baseBlocks(report, "Executive Security Summary");
        section(blocks, "Recommended next steps");
        if (report.executiveSummary().recommendedNextSteps().isEmpty()) {
            body(blocks, "No open findings require action right now.", 12);
        } else {
            for (ReportNextStep step : report.executiveSummary().recommendedNextSteps()) {
                body(blocks, step.order() + ". " + step.title() + " [" + step.severity().getValue()
                        + ", " + step.effort().level() + ", " + step.effort().estimate() + "]", 12);
                body(blocks, step.action(), 24);
            }
        }
        return renderBlocks(blocks);
    }

    public byte[] renderFull(ReportData report) {
        List<TextBlock> blocks = baseBlocks(report, "Full Scan Report");
        section(blocks, "Findings");
        if (report.findings().isEmpty()) {
            body(blocks, "No findings.", 12);
        } else {
            for (ReportFinding finding : report.findings()) {
                body(blocks, (finding.suggestedFixOrder() == null ? "-" : finding.suggestedFixOrder())
                        + ". " + finding.title() + " [" + finding.severity().getValue() + ", "
                        + finding.status().getValue() + "]", 12);
                body(blocks, "Affected: " + finding.affected(), 24);
                body(blocks, "Effort: " + finding.effort().level() + " - " + finding.effort().estimate(), 24);
                body(blocks, "Suggested fix: " + finding.suggestedFix(), 24);
                spacer(blocks, 5);
            }
        }
        return renderBlocks(blocks);
    }

    private List<TextBlock> baseBlocks(ReportData report, String title) {
        List<TextBlock> blocks = new ArrayList<>();
        title(blocks, title);
        subtitle(blocks, report.site().name() + " - " + report.site().url());
        spacer(blocks, 8);
        body(blocks, "Generated: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(report.generatedAt()), 0);
        body(blocks, "Scan status: " + report.scanStatus().getValue(), 0);
        body(blocks, "Risk level: " + report.executiveSummary().riskLevel(), 0);
        body(blocks, "Safe to launch: " + report.safeToLaunch().status(), 0);
        section(blocks, "Safe to launch checklist");
        body(blocks, "Each selected scan category is listed with its launch-readiness result.", 0);
        for (LaunchChecklistItem item : report.safeToLaunch().items()) {
            body(blocks, item.label() + ": " + item.result() + " - " + item.reason(), 12);
        }
        section(blocks, "Summary");
        body(blocks, "Open findings: " + report.executiveSummary().openFindings()
                + " of " + report.executiveSummary().totalFindings(), 0);
        long fixedOrIgnored = report.findings().stream()
                .filter(finding -> finding.status() != FindingStatus.OPEN)
                .count();
        if (fixedOrIgnored > 0) {
            body(blocks, "Resolved or ignored findings: " + fixedOrIgnored, 0);
        }
        return blocks;
    }

    private byte[] renderBlocks(List<TextBlock> blocks) {
        List<Page> pages = paginate(blocks);
        List<byte[]> objects = pdfObjects(pages);
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.UTF_8).length);
            pdf.append(i + 1).append(" 0 obj\n")
                    .append(new String(objects.get(i), StandardCharsets.UTF_8))
                    .append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<Page> paginate(List<TextBlock> blocks) {
        List<Page> pages = new ArrayList<>();
        Page current = new Page();
        int y = TOP;
        for (TextBlock block : blocks) {
            if (block.text().isEmpty()) {
                y -= block.height();
                continue;
            }
            int blockHeight = block.height();
            if (y - blockHeight < BOTTOM && !current.lines().isEmpty()) {
                pages.add(current);
                current = new Page();
                y = TOP;
            }
            current.lines().add(new PositionedText(block, y));
            y -= blockHeight;
        }
        pages.add(current);
        return pages;
    }

    private List<byte[]> pdfObjects(List<Page> pages) {
        StringBuilder kids = new StringBuilder();
        for (int index = 0; index < pages.size(); index++) {
            kids.append(4 + index * 2).append(" 0 R ");
        }

        List<byte[]> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.UTF_8));
        objects.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>")
                .getBytes(StandardCharsets.UTF_8));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.UTF_8));

        for (int index = 0; index < pages.size(); index++) {
            int pageId = 4 + index * 2;
            int contentId = pageId + 1;
            String content = contentForPage(pages.get(index), index + 1, pages.size());
            byte[] stream = content.getBytes(StandardCharsets.UTF_8);
            objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "] "
                    + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + contentId + " 0 R >>")
                    .getBytes(StandardCharsets.UTF_8));
            objects.add(("<< /Length " + stream.length + " >>\nstream\n" + content + "endstream")
                    .getBytes(StandardCharsets.UTF_8));
        }
        return objects;
    }

    private String contentForPage(Page page, int pageNumber, int pageCount) {
        StringBuilder content = new StringBuilder();
        for (PositionedText line : page.lines()) {
            TextBlock block = line.block();
            content.append("BT\n/F1 ").append(block.fontSize()).append(" Tf\n")
                    .append(LEFT + block.indent()).append(" ").append(line.y()).append(" Td\n")
                    .append("(").append(escapePdf(block.text())).append(") Tj\nET\n");
        }
        content.append("BT\n/F1 8 Tf\n").append(LEFT).append(" 30 Td\n(Page ")
                .append(pageNumber).append(" of ").append(pageCount).append(") Tj\nET\n");
        return content.toString();
    }

    private void title(List<TextBlock> blocks, String text) {
        blocks.add(new TextBlock(text, 18, 0, 24));
    }

    private void subtitle(List<TextBlock> blocks, String text) {
        blocks.add(new TextBlock(text, 11, 0, 17));
    }

    private void section(List<TextBlock> blocks, String text) {
        spacer(blocks, 8);
        blocks.add(new TextBlock(text, 13, 0, 20));
    }

    private void body(List<TextBlock> blocks, String text, int indent) {
        int wrap = indent == 0 ? BODY_WRAP : INDENT_WRAP;
        for (String line : wrap(text, wrap)) {
            blocks.add(new TextBlock(line, 10, indent, LINE_HEIGHT));
        }
    }

    private void spacer(List<TextBlock> blocks, int height) {
        blocks.add(new TextBlock("", 10, 0, height));
    }

    private List<String> wrap(String line, int maxLength) {
        List<String> wrapped = new ArrayList<>();
        String remaining = line == null ? "" : normalize(line).strip();
        if (remaining.isEmpty()) {
            return List.of("");
        }
        while (remaining.length() > maxLength) {
            int split = remaining.lastIndexOf(' ', maxLength);
            if (split < 24) {
                split = maxLength;
            }
            wrapped.add(remaining.substring(0, split).strip());
            remaining = remaining.substring(split).strip();
        }
        wrapped.add(remaining);
        return wrapped;
    }

    private String normalize(String value) {
        return value.replace("’", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("–", "-")
                .replace("—", "-");
    }

    private String escapePdf(String value) {
        return normalize(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private record TextBlock(String text, int fontSize, int indent, int height) {
    }

    private record PositionedText(TextBlock block, int y) {
    }

    private record Page(List<PositionedText> lines) {
        private Page() {
            this(new ArrayList<>());
        }
    }
}

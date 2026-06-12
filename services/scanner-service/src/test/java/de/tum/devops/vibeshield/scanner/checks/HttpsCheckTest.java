package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpsCheckTest {

    private final HttpsCheck check = new HttpsCheck();
    private static final URI TARGET = URI.create("https://shop.example.org/");

    @Test
    void cleanSite_redirectingHttpAndSendingHsts_hasNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("http://shop.example.org/", 301,
                        Map.of("Location", "https://shop.example.org/"), "")
                .respond("https://shop.example.org/", 200,
                        Map.of("Strict-Transport-Security", "max-age=31536000"), "<html/>");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void plainHttpServingContent_isHighFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("http://shop.example.org/", 200, Map.of(), "<html/>")
                .respond("https://shop.example.org/", 200,
                        Map.of("Strict-Transport-Security", "max-age=31536000"), "<html/>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getTitle()).contains("unencrypted HTTP");
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void httpsUnreachable_isCriticalFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("http://shop.example.org/", 200, Map.of(), "<html/>")
                .unreachable("https://shop.example.org/");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings)
                .extracting(ScannerFinding::getSeverity)
                .contains(Severity.CRITICAL);
    }

    @Test
    void missingHsts_isMediumFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("http://shop.example.org/", 301,
                        Map.of("Location", "https://shop.example.org/"), "")
                .respond("https://shop.example.org/", 200, Map.of(), "<html/>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getTitle()).contains("Strict-Transport-Security");
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.MEDIUM);
    }
}

package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeadersCheckTest {

    private final HeadersCheck check = new HeadersCheck();
    private static final URI TARGET = URI.create("https://shop.example.org/");

    @Test
    void fullyHardenedSite_hasNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher().respond("https://shop.example.org/", 200,
                Map.of(
                        "Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'",
                        "X-Content-Type-Options", "nosniff",
                        "Referrer-Policy", "strict-origin-when-cross-origin",
                        "Permissions-Policy", "camera=()"),
                "<html/>");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void bareSite_reportsEveryMissingHeader() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(), "<html/>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings)
                .extracting(ScannerFinding::getTitle)
                .anyMatch(title -> title.contains("Content-Security-Policy"))
                .anyMatch(title -> title.contains("clickjacking"))
                .anyMatch(title -> title.contains("X-Content-Type-Options"))
                .anyMatch(title -> title.contains("Referrer-Policy"))
                .anyMatch(title -> title.contains("Permissions-Policy"));
        assertThat(findings).hasSize(5);
    }

    @Test
    void xFrameOptionsAloneSatisfiesFramingProtection() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher().respond("https://shop.example.org/", 200,
                Map.of("X-Frame-Options", "DENY"), "<html/>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings)
                .extracting(ScannerFinding::getTitle)
                .noneMatch(title -> title.contains("clickjacking"));
    }

    @Test
    void unreachableTarget_yieldsNoFindings() {
        assertThat(check.run(TARGET, new FakeSiteFetcher())).isEmpty();
    }
}

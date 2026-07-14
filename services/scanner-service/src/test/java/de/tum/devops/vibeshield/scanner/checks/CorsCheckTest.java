package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsCheckTest {

    private final CorsCheck check = new CorsCheck();
    private static final URI TARGET = URI.create("https://shop.example.org/");

    @Test
    void reflectedOriginWithCredentials_isCriticalFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200,
                        Map.of("Access-Control-Allow-Origin", "https://vibeshield-cors-probe.invalid",
                                "Access-Control-Allow-Credentials", "true"),
                        "hi");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(findings.get(0).getTitle()).contains("allows credentials");
    }

    @Test
    void reflectedOriginWithoutCredentials_isMediumFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200,
                        Map.of("Access-Control-Allow-Origin", "https://vibeshield-cors-probe.invalid"),
                        "hi");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    void bareWildcardOrigin_producesNoFinding() {
        // A public API intentionally serving Access-Control-Allow-Origin: * is
        // normal, documented practice — not a misconfiguration on its own.
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200,
                        Map.of("Access-Control-Allow-Origin", "*"), "hi");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void fixedAllowListOrigin_producesNoFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200,
                        Map.of("Access-Control-Allow-Origin", "https://trusted-partner.example"),
                        "hi");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void noCorsHeaderAtAll_producesNoFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(), "hi");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void unreachableHomepage_producesNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .unreachable("https://shop.example.org/");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }
}

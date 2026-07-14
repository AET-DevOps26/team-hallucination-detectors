package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPathsCheckTest {

    private final AdminPathsCheck check = new AdminPathsCheck();
    private static final URI TARGET = URI.create("https://shop.example.org/");

    @Test
    void exposedWpAdmin_isMediumFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/wp-admin", 200, Map.of(),
                        "<html><body><form><input type=\"password\"> Log in to WordPress"
                                + "</form></body></html>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(findings.get(0).getAffected()).endsWith("/wp-admin");
    }

    @Test
    void exposedPhpMyAdmin_isHighFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/phpmyadmin", 200, Map.of(),
                        "<html><body>phpMyAdmin - username / password login</body></html>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void spaShellAnsweringEveryPath_doesNotProduceFalsePositives() {
        // SPA hosts answer unknown paths (including our baseline probe) 200 with the
        // same app shell — identical bodies must not be mistaken for a real panel,
        // even if the shell happens to mention "admin" somewhere (app name, footer, etc).
        String appShell = "<!doctype html><html><head><title>Shop Admin Dashboard</title>"
                + "</head></html>";
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/vibeshield-nonexistent-probe-4f3c9a1e", 200,
                        Map.of(), appShell)
                .respond("https://shop.example.org/wp-admin", 200, Map.of(), appShell)
                .respond("https://shop.example.org/admin", 200, Map.of(), appShell)
                .respond("https://shop.example.org/login", 200, Map.of(), appShell);

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void genuineTwoHundredWithoutLoginMarkers_doesNotProduceAFinding() {
        // Differing from the baseline is necessary but not sufficient — the body must
        // also look like an actual login/admin surface, not just any distinct page.
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/admin", 200, Map.of(),
                        "<html><body>Nothing to see here</body></html>");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void notFoundResponses_produceNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/admin", 404, Map.of(), "not found");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }
}

package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveFilesCheckTest {

    private final SensitiveFilesCheck check = new SensitiveFilesCheck();
    private static final URI TARGET = URI.create("https://shop.example.org/");

    @Test
    void exposedEnvFile_isCriticalFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/.env", 200, Map.of(),
                        "DATABASE_URL=postgres://u:p@db/prod\nOPENAI_API_KEY=sk-123");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(findings.get(0).getAffected()).endsWith("/.env");
    }

    @Test
    void exposedGitMetadata_isHighFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/.git/config", 200, Map.of(),
                        "[core]\n\trepositoryformatversion = 0")
                .respond("https://shop.example.org/.git/HEAD", 200, Map.of(),
                        "ref: refs/heads/main");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(2);
        assertThat(findings).allSatisfy(finding ->
                assertThat(finding.getSeverity()).isEqualTo(Severity.HIGH));
    }

    @Test
    void spaShellAnsweringEveryPath_doesNotProduceFalsePositives() {
        // SPA hosts answer unknown paths 200 with the app HTML — the content
        // heuristic must reject that.
        String appShell = "<!doctype html><html><head><title>Shop</title></head></html>";
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/.env", 200, Map.of(), appShell)
                .respond("https://shop.example.org/.git/config", 200, Map.of(), appShell)
                .respond("https://shop.example.org/.git/HEAD", 200, Map.of(), appShell)
                .respond("https://shop.example.org/.DS_Store", 200, Map.of(), appShell);

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void notFoundResponses_produceNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/.env", 404, Map.of(), "not found");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }
}

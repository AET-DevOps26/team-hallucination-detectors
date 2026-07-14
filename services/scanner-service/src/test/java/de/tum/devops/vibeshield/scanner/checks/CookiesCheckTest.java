package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CookiesCheckTest {

    private final CookiesCheck check = new CookiesCheck();
    private static final URI HTTPS_TARGET = URI.create("https://shop.example.org/");
    private static final URI HTTP_TARGET = URI.create("http://shop.example.org/");

    @Test
    void cookieMissingAllThreeAttributes_producesThreeFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respondWithHeaders("https://shop.example.org/", 200,
                        Map.of("Set-Cookie", List.of("session_id=abc123; Path=/")), "hi");

        List<ScannerFinding> findings = check.run(HTTPS_TARGET, fetcher);

        assertThat(findings).hasSize(3);
        assertThat(findings).extracting(ScannerFinding::getSeverity)
                .containsExactlyInAnyOrder(Severity.MEDIUM, Severity.MEDIUM, Severity.LOW);
        assertThat(findings).allSatisfy(finding ->
                assertThat(finding.getExplanation()).contains("session_id"));
    }

    @Test
    void fullyFlaggedCookie_producesNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respondWithHeaders("https://shop.example.org/", 200,
                        Map.of("Set-Cookie",
                                List.of("session_id=abc123; Secure; HttpOnly; SameSite=Lax")),
                        "hi");

        assertThat(check.run(HTTPS_TARGET, fetcher)).isEmpty();
    }

    @Test
    void secureAttribute_isNotCheckedOnPlainHttpSite() {
        // HttpsCheck already reports the bigger problem (no HTTPS at all); flagging
        // every cookie for lacking Secure on top of that would be redundant noise.
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respondWithHeaders("http://shop.example.org/", 200,
                        Map.of("Set-Cookie", List.of("session_id=abc123; HttpOnly; SameSite=Lax")),
                        "hi");

        List<ScannerFinding> findings = check.run(HTTP_TARGET, fetcher);

        assertThat(findings).isEmpty();
    }

    @Test
    void multipleCookiesMissingSameAttribute_areBundledIntoOneFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respondWithHeaders("https://shop.example.org/", 200,
                        Map.of("Set-Cookie", List.of(
                                "session_id=abc123; Secure; SameSite=Lax",
                                "cart_id=xyz987; Secure; SameSite=Lax")),
                        "hi");

        List<ScannerFinding> findings = check.run(HTTPS_TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getTitle()).contains("HttpOnly");
        assertThat(findings.get(0).getExplanation())
                .contains("session_id")
                .contains("cart_id");
    }

    @Test
    void noCookiesSet_producesNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(), "hi");

        assertThat(check.run(HTTPS_TARGET, fetcher)).isEmpty();
    }

    @Test
    void unreachableHomepage_producesNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .unreachable("https://shop.example.org/");

        assertThat(check.run(HTTPS_TARGET, fetcher)).isEmpty();
    }
}

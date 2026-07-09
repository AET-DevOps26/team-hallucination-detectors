package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecretsCheckTest {

    private final SecretsCheck check = new SecretsCheck();
    private static final URI TARGET = URI.create("https://shop.example.org/");

    // Assembled at runtime, not written as one contiguous literal, so this
    // synthetic fixture doesn't itself look like a real credential to secret
    // scanners (e.g. GitHub push protection) reading the source file.
    private static String fakeStripeSecretKey() {
        return "sk" + "_live_" + "NOTAREALKEY0000000000TESTONLY";
    }

    @Test
    void stripeSecretKeyOnHomepage_isCriticalFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<script>const stripe = '" + fakeStripeSecretKey() + "';</script>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(findings.get(0).getTitle()).contains("Stripe secret key");
        assertThat(findings.get(0).getAffected()).isEqualTo("https://shop.example.org/");
    }

    @Test
    void awsAccessKeyId_isHighFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "window.config = { awsKey: 'AKIAABCDEFGHIJKLMNOP' };");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(findings.get(0).getTitle()).contains("AWS access key");
    }

    @Test
    void privateKeyBlock_isCriticalFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "-----BEGIN RSA PRIVATE KEY-----\nMIIBOgIBAAJBAK...\n-----END RSA PRIVATE KEY-----");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(findings.get(0).getTitle()).contains("Private key");
    }

    @Test
    void supabaseUrlAndKeyTogether_isInfoFindingAboutRls() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                + ".eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIn0"
                + ".dGhpc2lzbm90YXJlYWxzaWduYXR1cmU";
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(), "<script>"
                        + "createClient('https://abcdefghijklmno.supabase.co', '" + jwt + "')"
                        + "</script>");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.INFO);
        assertThat(findings.get(0).getTitle()).contains("Supabase");
        assertThat(findings.get(0).getSuggestedFix()).contains("Row Level Security");
    }

    @Test
    void supabaseUrlWithoutKey_producesNoFinding() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<a href=\"https://abcdefghijklmno.supabase.co\">status page</a>");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void secretInSameOriginScriptAsset_isFoundAndAttributedToThatAsset() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<html><head><script src=\"/assets/main.js\"></script></head></html>")
                .respond("https://shop.example.org/assets/main.js", 200, Map.of(),
                        "const key = '" + fakeStripeSecretKey() + "';");

        List<ScannerFinding> findings = check.run(TARGET, fetcher);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getAffected())
                .isEqualTo("https://shop.example.org/assets/main.js");
    }

    @Test
    void crossOriginScriptAsset_isNeverFetchedOrScanned() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<script src=\"https://cdn.example.net/vendor.js\"></script>")
                // Scripted so that IF the check fetched it, a finding would appear —
                // its absence proves the cross-origin asset was never fetched.
                .respond("https://cdn.example.net/vendor.js", 200, Map.of(),
                        "const key = '" + fakeStripeSecretKey() + "';");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void cleanHomepage_producesNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<html><body>Welcome to the shop</body></html>");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }

    @Test
    void unreachableHomepage_producesNoFindings() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .unreachable("https://shop.example.org/");

        assertThat(check.run(TARGET, fetcher)).isEmpty();
    }
}

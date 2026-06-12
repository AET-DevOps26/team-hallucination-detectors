package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.http.FetchResult;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static de.tum.devops.vibeshield.scanner.checks.Findings.finding;

/**
 * Security response headers on the start page: CSP, clickjacking protection,
 * MIME sniffing, referrer leakage, and browser-feature exposure.
 */
@Component
public class HeadersCheck implements SecurityCheck {

    @Override
    public ScanCheck type() {
        return ScanCheck.HEADERS;
    }

    @Override
    public List<ScannerFinding> run(URI target, SiteFetcher fetcher) {
        FetchResult response = fetcher.fetch(target);
        if (!response.reachable()) {
            return List.of();
        }

        List<ScannerFinding> findings = new ArrayList<>();
        String affected = target.toString();

        boolean hasCsp = response.header("Content-Security-Policy").isPresent();
        if (!hasCsp) {
            findings.add(finding(type(),
                    "Missing Content-Security-Policy header",
                    Severity.MEDIUM,
                    affected,
                    "Without a Content-Security-Policy, any script that sneaks into your "
                            + "page (e.g. through a compromised dependency or injected content) "
                            + "runs with full access to your users' sessions.",
                    "Add a Content-Security-Policy response header. Start with "
                            + "\"default-src 'self'\" and add the external sources your site "
                            + "actually uses."));
        }

        boolean framingProtected = response.header("X-Frame-Options").isPresent()
                || response.header("Content-Security-Policy")
                        .map(csp -> csp.toLowerCase().contains("frame-ancestors"))
                        .orElse(false);
        if (!framingProtected) {
            findings.add(finding(type(),
                    "Site can be embedded in other pages (clickjacking)",
                    Severity.MEDIUM,
                    affected,
                    "Other websites can load your site invisibly inside a frame and trick "
                            + "your users into clicking buttons they cannot see.",
                    "Send the response header 'X-Frame-Options: DENY' (or add "
                            + "\"frame-ancestors 'none'\" to your Content-Security-Policy)."));
        }

        boolean noSniff = response.header("X-Content-Type-Options")
                .map(value -> value.equalsIgnoreCase("nosniff"))
                .orElse(false);
        if (!noSniff) {
            findings.add(finding(type(),
                    "Missing X-Content-Type-Options header",
                    Severity.LOW,
                    affected,
                    "Browsers may second-guess file types and execute uploads or assets as "
                            + "scripts even when they should not be.",
                    "Send the response header 'X-Content-Type-Options: nosniff'."));
        }

        if (response.header("Referrer-Policy").isEmpty()) {
            findings.add(finding(type(),
                    "Missing Referrer-Policy header",
                    Severity.LOW,
                    affected,
                    "When users follow links away from your site, the full page address "
                            + "(which can contain tokens or private paths) is shared with the "
                            + "destination site.",
                    "Send the response header 'Referrer-Policy: strict-origin-when-cross-origin'."));
        }

        if (response.header("Permissions-Policy").isEmpty()) {
            findings.add(finding(type(),
                    "Missing Permissions-Policy header",
                    Severity.INFO,
                    affected,
                    "Browser features like camera, microphone, and location are not "
                            + "explicitly restricted for embedded content.",
                    "Send the response header 'Permissions-Policy: camera=(), microphone=(), "
                            + "geolocation=()' and allow only what your site needs."));
        }

        return findings;
    }
}

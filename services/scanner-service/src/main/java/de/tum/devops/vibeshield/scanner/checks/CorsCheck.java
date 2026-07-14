package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.http.FetchResult;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static de.tum.devops.vibeshield.scanner.checks.Findings.finding;

/**
 * Sends the homepage request with a synthetic, never-real Origin header and
 * checks whether Access-Control-Allow-Origin reflects it back exactly. A fixed
 * allow-list or an absent CORS policy would never echo this made-up origin, so
 * an exact match is unambiguous evidence the server reflects whatever Origin it
 * receives rather than checking it against a list — the actual misconfiguration,
 * not merely having CORS enabled. A bare wildcard (Access-Control-Allow-Origin:
 * *) is deliberately NOT flagged: it's the intended, documented way to serve a
 * public API, not a vulnerability on its own.
 */
@Component
public class CorsCheck implements SecurityCheck {

    /** RFC 2606 reserves .invalid — guaranteed never a real, registrable origin. */
    private static final String PROBE_ORIGIN = "https://vibeshield-cors-probe.invalid";

    @Override
    public ScanCheck type() {
        return ScanCheck.CORS;
    }

    @Override
    public List<ScannerFinding> run(URI target, SiteFetcher fetcher) {
        FetchResult response = fetcher.fetch(target, Map.of("Origin", PROBE_ORIGIN));
        if (!response.reachable()) {
            return List.of();
        }

        boolean reflectsAnyOrigin = response.header("Access-Control-Allow-Origin")
                .map(value -> value.equalsIgnoreCase(PROBE_ORIGIN))
                .orElse(false);
        if (!reflectsAnyOrigin) {
            return List.of();
        }

        boolean allowsCredentials = response.header("Access-Control-Allow-Credentials")
                .map(value -> value.equalsIgnoreCase("true"))
                .orElse(false);

        if (allowsCredentials) {
            return List.of(finding(type(),
                    "CORS policy reflects any origin and allows credentials",
                    Severity.CRITICAL,
                    target.toString(),
                    "This site reflects whatever Origin a request sends back in "
                            + "Access-Control-Allow-Origin, and also sends "
                            + "Access-Control-Allow-Credentials: true. Together, this lets any "
                            + "website make authenticated requests to this site using a visitor's "
                            + "logged-in session and read the response back — effectively full "
                            + "account takeover for anyone who visits a malicious page while "
                            + "logged in.",
                    "Replace the dynamic origin reflection with a fixed allow-list of the exact "
                            + "origins that legitimately need cross-origin access. Never combine a "
                            + "reflected or wildcard origin with "
                            + "Access-Control-Allow-Credentials: true."));
        }

        return List.of(finding(type(),
                "CORS policy reflects any origin",
                Severity.MEDIUM,
                target.toString(),
                "This site reflects whatever Origin a request sends back in "
                        + "Access-Control-Allow-Origin instead of checking it against a list of "
                        + "allowed sites. Any website can make cross-origin requests here and "
                        + "read the response, which can leak data that wasn't meant to be public.",
                "Replace the dynamic origin reflection with a fixed allow-list of the exact "
                        + "origins that legitimately need cross-origin access."));
    }
}

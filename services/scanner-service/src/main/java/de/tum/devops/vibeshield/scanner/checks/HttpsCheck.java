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
 * Transport security: is plain-HTTP traffic possible, does it redirect to HTTPS,
 * and does the HTTPS response pin browsers to HTTPS via HSTS?
 */
@Component
public class HttpsCheck implements SecurityCheck {

    @Override
    public ScanCheck type() {
        return ScanCheck.HTTPS;
    }

    @Override
    public List<ScannerFinding> run(URI target, SiteFetcher fetcher) {
        List<ScannerFinding> findings = new ArrayList<>();
        URI httpVariant = withScheme(target, "http");
        URI httpsVariant = withScheme(target, "https");

        FetchResult httpResponse = fetcher.fetch(httpVariant);
        if (httpResponse.reachable()) {
            if (isRedirectToHttps(httpResponse)) {
                // Good: HTTP exists only to bounce visitors to HTTPS.
            } else {
                findings.add(finding(type(),
                        "Site is reachable over unencrypted HTTP",
                        Severity.HIGH,
                        httpVariant.toString(),
                        "Your site answers normal requests over plain HTTP. Anyone on the same "
                                + "network (e.g. public Wi-Fi) can read or modify what your "
                                + "visitors see, including passwords they type.",
                        "Configure your hosting to redirect all HTTP traffic to HTTPS "
                                + "permanently (a 301 redirect to the https:// address)."));
            }
        }

        FetchResult httpsResponse = fetcher.fetch(httpsVariant);
        if (!httpsResponse.reachable()) {
            findings.add(finding(type(),
                    "HTTPS is not available",
                    Severity.CRITICAL,
                    httpsVariant.toString(),
                    "Your site could not be reached over HTTPS at all, so every visit is "
                            + "unencrypted. Browsers will warn visitors that the site is not secure.",
                    "Enable HTTPS in your hosting settings. Most platforms (Vercel, Netlify, "
                            + "Lovable, Replit) provide free certificates with one click."));
        } else if (httpsResponse.header("Strict-Transport-Security").isEmpty()) {
            findings.add(finding(type(),
                    "Missing Strict-Transport-Security header",
                    Severity.MEDIUM,
                    httpsVariant.toString(),
                    "Without this header, browsers may still try plain HTTP on the next "
                            + "visit, leaving a window for connection hijacking.",
                    "Send the response header 'Strict-Transport-Security: max-age=31536000' "
                            + "on all HTTPS responses."));
        }

        return findings;
    }

    private boolean isRedirectToHttps(FetchResult response) {
        if (response.status() < 300 || response.status() >= 400) {
            return false;
        }
        return response.header("Location")
                .map(location -> location.toLowerCase().startsWith("https://"))
                .orElse(false);
    }

    private URI withScheme(URI uri, String scheme) {
        return URI.create(scheme + "://" + uri.getRawAuthority()
                + (uri.getRawPath() == null ? "" : uri.getRawPath()));
    }
}

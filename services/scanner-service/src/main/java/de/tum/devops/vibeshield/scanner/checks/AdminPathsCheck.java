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
import java.util.Locale;

import static de.tum.devops.vibeshield.scanner.checks.Findings.finding;

/**
 * Probes a fixed list of well-known admin/login paths. Unlike
 * {@link SensitiveFilesCheck}, these paths have no self-evidently "genuine"
 * content, so a finding requires the response to differ from a baseline fetch
 * of a definitely-nonexistent path (defeats SPA hosts that 200 every path with
 * the app shell) AND look like an actual login/admin surface.
 */
@Component
public class AdminPathsCheck implements SecurityCheck {

    private record Probe(String path, String title, Severity severity, String explanation,
                         String suggestedFix) {
    }

    /** Deliberately-unrouted path used to fingerprint the site's generic fallback response. */
    private static final String BASELINE_PATH = "/vibeshield-nonexistent-probe-4f3c9a1e";

    private static final List<String> LOGIN_MARKERS = List.of(
            "password", "sign in", "signin", "log in", "login", "username", "admin"
    );

    private static final List<Probe> PROBES = List.of(
            new Probe("/wp-admin",
                    "WordPress admin panel is publicly reachable",
                    Severity.MEDIUM,
                    "Your WordPress admin login is exposed to anyone on the internet. It's a "
                            + "constant target for automated credential-stuffing and brute-force "
                            + "login attempts.",
                    "Restrict /wp-admin to known IPs at the hosting/proxy level, enforce a strong "
                            + "unique password and two-factor authentication, and consider a login "
                            + "rate-limiting plugin."),
            new Probe("/wp-login.php",
                    "WordPress login page is publicly reachable",
                    Severity.MEDIUM,
                    "Your WordPress login form is exposed to anyone on the internet. It's a "
                            + "constant target for automated credential-stuffing and brute-force "
                            + "login attempts.",
                    "Restrict /wp-login.php to known IPs at the hosting/proxy level, enforce a "
                            + "strong unique password and two-factor authentication, and consider a "
                            + "login rate-limiting plugin."),
            new Probe("/phpmyadmin",
                    "phpMyAdmin database console is publicly reachable",
                    Severity.HIGH,
                    "A database administration console is exposed to the internet. If its "
                            + "credentials are weak or default, this is a direct path to reading or "
                            + "deleting your entire database.",
                    "Remove public access to phpMyAdmin, or restrict it to known IPs or a VPN, and "
                            + "make sure it does not use default credentials."),
            new Probe("/admin",
                    "Admin panel is publicly reachable",
                    Severity.MEDIUM,
                    "An admin login page is exposed to anyone on the internet. If it isn't meant "
                            + "to be public, attackers now know exactly where to aim "
                            + "credential-stuffing and brute-force attempts.",
                    "Confirm the admin panel is meant to be publicly reachable. If so, enforce a "
                            + "strong unique password and two-factor authentication; if not, "
                            + "restrict access at the hosting/proxy level."),
            new Probe("/admin/login",
                    "Admin login page is publicly reachable",
                    Severity.MEDIUM,
                    "An admin login page is exposed to anyone on the internet. If it isn't meant "
                            + "to be public, attackers now know exactly where to aim "
                            + "credential-stuffing and brute-force attempts.",
                    "Confirm the admin panel is meant to be publicly reachable. If so, enforce a "
                            + "strong unique password and two-factor authentication; if not, "
                            + "restrict access at the hosting/proxy level."),
            new Probe("/administrator",
                    "Admin login page is publicly reachable",
                    Severity.MEDIUM,
                    "An admin login page is exposed to anyone on the internet. If it isn't meant "
                            + "to be public, attackers now know exactly where to aim "
                            + "credential-stuffing and brute-force attempts.",
                    "Confirm the admin panel is meant to be publicly reachable. If so, enforce a "
                            + "strong unique password and two-factor authentication; if not, "
                            + "restrict access at the hosting/proxy level."),
            new Probe("/login",
                    "Login page is publicly reachable",
                    Severity.INFO,
                    "A login form was found at a predictable path. That's normal for most sites, "
                            + "but worth confirming it's rate-limited so it can't be brute-forced.",
                    "Make sure failed login attempts are rate-limited or locked out after repeated "
                            + "failures, and that the form is only ever served over HTTPS.")
    );

    @Override
    public ScanCheck type() {
        return ScanCheck.ADMIN_PATHS;
    }

    @Override
    public List<ScannerFinding> run(URI target, SiteFetcher fetcher) {
        FetchResult baseline = fetcher.fetch(target.resolve(BASELINE_PATH));
        String baselineBody = baseline.reachable() ? baseline.bodySnippet() : "";

        List<ScannerFinding> findings = new ArrayList<>();
        for (Probe probe : PROBES) {
            URI probeUri = target.resolve(probe.path());
            FetchResult response = fetcher.fetch(probeUri);
            if (response.reachable() && response.status() == 200
                    && looksLikeGenuineAdminSurface(response.bodySnippet(), baselineBody)) {
                findings.add(finding(type(), probe.title(), probe.severity(),
                        probeUri.toString(), probe.explanation(), probe.suggestedFix()));
            }
        }
        return findings;
    }

    private boolean looksLikeGenuineAdminSurface(String body, String baselineBody) {
        if (body.isBlank() || body.equals(baselineBody)) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return LOGIN_MARKERS.stream().anyMatch(lower::contains);
    }
}

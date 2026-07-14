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
import java.util.function.Predicate;

import static de.tum.devops.vibeshield.scanner.checks.Findings.finding;

/**
 * Inspects every Set-Cookie header on the homepage response for the three
 * attributes that actually matter for a cookie's safety: Secure, HttpOnly, and
 * SameSite. Evaluated uniformly across every cookie the site sets (not just
 * ones that look like session tokens) — guessing which cookie names are
 * "important" is fragile, and the fix is the same either way: set the
 * attribute unless you have a specific reason not to.
 *
 * <p>One finding per missing attribute (naming every affected cookie), not one
 * finding per cookie, so a site with many cookies doesn't flood the dashboard.
 */
@Component
public class CookiesCheck implements SecurityCheck {

    private record ParsedCookie(String name, boolean secure, boolean httpOnly, boolean hasSameSite) {
    }

    @Override
    public ScanCheck type() {
        return ScanCheck.COOKIES;
    }

    @Override
    public List<ScannerFinding> run(URI target, SiteFetcher fetcher) {
        FetchResult response = fetcher.fetch(target);
        if (!response.reachable()) {
            return List.of();
        }

        List<ParsedCookie> cookies = response.headerValues("Set-Cookie").stream()
                .map(CookiesCheck::parse)
                .filter(cookie -> !cookie.name().isBlank())
                .toList();
        if (cookies.isEmpty()) {
            return List.of();
        }

        List<ScannerFinding> findings = new ArrayList<>();

        // The Secure attribute only matters when the page itself is HTTPS — a
        // plain-HTTP site has a bigger problem that HttpsCheck already reports.
        if ("https".equalsIgnoreCase(target.getScheme())) {
            addIfAnyMissing(findings, target, cookies, ParsedCookie::secure,
                    "Cookies set without the Secure attribute",
                    Severity.MEDIUM,
                    "sent without the Secure attribute",
                    "Without it, a cookie can also be sent over an unencrypted connection if "
                            + "one is ever available, letting anyone on the network read it.",
                    "Add the Secure attribute to every cookie your site sends, e.g. "
                            + "'Set-Cookie: name=value; Secure; ...'. Most frameworks have a "
                            + "one-line config flag for this.");
        }

        addIfAnyMissing(findings, target, cookies, ParsedCookie::httpOnly,
                "Cookies set without the HttpOnly attribute",
                Severity.MEDIUM,
                "readable by JavaScript running on the page",
                "If an attacker ever gets a script to run on your site, they can steal these "
                        + "cookies directly.",
                "Add the HttpOnly attribute to every cookie that doesn't need to be read by "
                        + "your own JavaScript — session and login cookies almost never do. "
                        + "e.g. 'Set-Cookie: name=value; HttpOnly; ...'.");

        addIfAnyMissing(findings, target, cookies, ParsedCookie::hasSameSite,
                "Cookies set without a SameSite attribute",
                Severity.LOW,
                "sent without a SameSite policy",
                "Without it, browsers may attach these cookies to requests that originate from "
                        + "other sites, which can be abused for cross-site request forgery.",
                "Add 'SameSite=Lax' (or 'Strict' for your most sensitive cookies) to every "
                        + "Set-Cookie response.");

        return findings;
    }

    private void addIfAnyMissing(List<ScannerFinding> findings, URI target,
                                 List<ParsedCookie> cookies, Predicate<ParsedCookie> hasAttribute,
                                 String title, Severity severity, String problem,
                                 String risk, String suggestedFix) {
        List<String> affected = cookies.stream()
                .filter(cookie -> !hasAttribute.test(cookie))
                .map(ParsedCookie::name)
                .toList();
        if (affected.isEmpty()) {
            return;
        }
        String explanation = "The following cookies are " + problem + ": "
                + String.join(", ", affected) + ". " + risk;
        findings.add(finding(type(), title, severity, target.toString(), explanation, suggestedFix));
    }

    private static ParsedCookie parse(String setCookieHeader) {
        String[] parts = setCookieHeader.split(";");
        String name = parts[0].split("=", 2)[0].trim();

        boolean secure = false;
        boolean httpOnly = false;
        boolean hasSameSite = false;
        for (int i = 1; i < parts.length; i++) {
            String attribute = parts[i].trim().toLowerCase(Locale.ROOT);
            if (attribute.equals("secure")) {
                secure = true;
            } else if (attribute.equals("httponly")) {
                httpOnly = true;
            } else if (attribute.startsWith("samesite")) {
                hasSameSite = true;
            }
        }
        return new ParsedCookie(name, secure, httpOnly, hasSameSite);
    }
}

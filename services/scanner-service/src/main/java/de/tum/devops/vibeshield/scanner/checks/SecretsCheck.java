package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.http.FetchResult;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.tum.devops.vibeshield.scanner.checks.Findings.finding;

/**
 * Scans the homepage and its same-origin {@code <script src>} assets for
 * hardcoded secrets. Deliberately passive: no attempt to validate a key against
 * its provider, and only same-origin script assets are fetched (never
 * third-party CDNs), which keeps this a surface-level content scan rather than
 * active probing.
 *
 * <p>Publishable/public-by-design values (Stripe {@code pk_live_}, bare Google
 * {@code AIza} keys, Firebase config objects) are intentionally NOT flagged —
 * providers document these as safe for client-side code, so treating them as a
 * leak would be a false claim. Supabase is the one exception: its anon key is
 * also meant to be client-side, but VibeShield's product promise is specifically
 * about surfacing what that key can reach, so it gets an informational finding
 * pointing at Row Level Security rather than a "you leaked a secret" claim.
 */
@Component
public class SecretsCheck implements SecurityCheck {

    private record Rule(String name, Pattern pattern, Severity severity, String title,
                        String explanation, String suggestedFix) {
    }

    private record Source(URI uri, String body) {
    }

    private static final int MAX_SCRIPT_ASSETS = 5;

    private static final Pattern SCRIPT_SRC =
            Pattern.compile("<script[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern SUPABASE_URL =
            Pattern.compile("https://[a-z0-9]{15,25}\\.supabase\\.co");

    private static final Pattern JWT_LIKE =
            Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");

    private static final List<Rule> RULES = List.of(
            new Rule("stripe-secret-key",
                    Pattern.compile("\\bsk_live_[0-9a-zA-Z]{10,}\\b"),
                    Severity.CRITICAL,
                    "Stripe secret key is exposed client-side",
                    "A Stripe secret key was found in code your visitors' browsers download. "
                            + "Anyone who finds it can create charges, issue refunds, and read your "
                            + "payment data as if they were you.",
                    "Remove the secret key from any client-side code immediately and roll it in "
                            + "the Stripe dashboard. Secret keys belong only on your server, never in "
                            + "code that ships to the browser."),
            new Rule("aws-access-key-id",
                    Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
                    Severity.HIGH,
                    "AWS access key is exposed client-side",
                    "An AWS access key ID was found in code your visitors' browsers download. If "
                            + "its matching secret key is nearby, this gives full access to whatever "
                            + "your AWS account can do.",
                    "Remove the key from any client-side code, deactivate it in the AWS IAM "
                            + "console, and issue a new one that only your server holds."),
            new Rule("private-key-block",
                    Pattern.compile("-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----"),
                    Severity.CRITICAL,
                    "Private key is exposed client-side",
                    "A private key block was found in code your visitors' browsers download. "
                            + "Private keys must never leave your server — once shipped to the "
                            + "browser, this one can no longer be considered secret.",
                    "Remove the key from any client-side code immediately and replace it with a "
                            + "newly generated one wherever it was used.")
    );

    @Override
    public ScanCheck type() {
        return ScanCheck.SECRETS;
    }

    @Override
    public List<ScannerFinding> run(URI target, SiteFetcher fetcher) {
        FetchResult homepage = fetcher.fetch(target);
        if (!homepage.reachable()) {
            return List.of();
        }

        // bodySnippet() is capped at HttpSiteFetcher.MAX_BODY_BYTES (16 KB), so a
        // secret past that offset in a large minified bundle is not scanned. Raising
        // the cap (or streaming) for script assets is tracked as a follow-up.
        List<Source> sources = new ArrayList<>();
        sources.add(new Source(target, homepage.bodySnippet()));
        for (URI scriptUri : sameOriginScriptUris(target, homepage.bodySnippet())) {
            FetchResult script = fetcher.fetch(scriptUri);
            if (script.reachable() && script.status() == 200) {
                sources.add(new Source(scriptUri, script.bodySnippet()));
            }
        }

        List<ScannerFinding> findings = new ArrayList<>();
        Set<String> matchedRules = new HashSet<>();
        boolean supabaseFlagged = false;

        for (Source source : sources) {
            for (Rule rule : RULES) {
                if (matchedRules.contains(rule.name())) {
                    continue;
                }
                if (rule.pattern().matcher(source.body()).find()) {
                    matchedRules.add(rule.name());
                    findings.add(finding(type(), rule.title(), rule.severity(),
                            source.uri().toString(), rule.explanation(), rule.suggestedFix()));
                }
            }
            // Co-occurrence in the SAME source, not just the same scan, is the signal:
            // a Supabase URL and a JWT-shaped key together strongly suggest a
            // supabase-js client() init, not two unrelated strings on the page.
            if (!supabaseFlagged && SUPABASE_URL.matcher(source.body()).find()
                    && JWT_LIKE.matcher(source.body()).find()) {
                supabaseFlagged = true;
                findings.add(finding(type(),
                        "Supabase project configuration is exposed client-side",
                        Severity.INFO,
                        source.uri().toString(),
                        "A Supabase project URL and API key were found in code your visitors' "
                                + "browsers download. That's expected — Supabase's public (anon) key "
                                + "is designed to be client-side — but it also means anyone can use "
                                + "it to query your database directly.",
                        "In the Supabase dashboard, enable Row Level Security (RLS) on every table "
                                + "and add policies that only allow the access each table actually "
                                + "needs. Without RLS, this key can read or write any row in any "
                                + "table."));
            }
        }

        return findings;
    }

    private List<URI> sameOriginScriptUris(URI target, String html) {
        List<URI> uris = new ArrayList<>();
        Matcher matcher = SCRIPT_SRC.matcher(html);
        while (matcher.find() && uris.size() < MAX_SCRIPT_ASSETS) {
            String src = matcher.group(1);
            if (src.startsWith("data:")) {
                continue;
            }
            URI resolved;
            try {
                resolved = target.resolve(src);
            } catch (IllegalArgumentException malformed) {
                continue;
            }
            if (isSameOrigin(target, resolved) && !uris.contains(resolved)) {
                uris.add(resolved);
            }
        }
        return uris;
    }

    private boolean isSameOrigin(URI target, URI candidate) {
        return target.getHost() != null && target.getHost().equalsIgnoreCase(candidate.getHost());
    }
}

package de.tum.devops.vibeshield.scanner.http;

import java.net.URI;
import java.util.Map;

/**
 * The scanner's only window onto the target site. Implementations must stay passive:
 * plain GET requests, bounded timeouts, a hard per-scan request budget, and an
 * identifying User-Agent — never any payload injection or active probing.
 */
public interface SiteFetcher {

    /**
     * GETs the URI without following redirects and returns the response (or
     * {@link FetchResult#unreachable()}). Body capture is limited to a snippet —
     * enough for content heuristics, never full downloads.
     *
     * @throws RequestBudgetExceededException when the per-scan request cap is spent
     */
    FetchResult fetch(URI uri);

    /**
     * Like {@link #fetch(URI)}, but with additional request headers merged in —
     * e.g. a synthetic Origin header to observe how the server's CORS policy
     * responds. Still a passive GET; counts against the same request budget.
     *
     * @throws RequestBudgetExceededException when the per-scan request cap is spent
     */
    FetchResult fetch(URI uri, Map<String, String> requestHeaders);
}

package de.tum.devops.vibeshield.scanner.http;

import java.net.URI;

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
}

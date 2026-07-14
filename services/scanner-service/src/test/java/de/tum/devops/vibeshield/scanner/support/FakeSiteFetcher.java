package de.tum.devops.vibeshield.scanner.support;

import de.tum.devops.vibeshield.scanner.http.FetchResult;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory {@link SiteFetcher}: tests script exact responses per URI, no network. */
public class FakeSiteFetcher implements SiteFetcher {

    private final Map<URI, FetchResult> responses = new HashMap<>();

    public FakeSiteFetcher respond(String uri, int status, Map<String, String> headers, String body) {
        Map<String, List<String>> multiHeaders = new HashMap<>();
        headers.forEach((name, value) -> multiHeaders.put(name, List.of(value)));
        responses.put(URI.create(uri), new FetchResult(true, status, multiHeaders, body));
        return this;
    }

    /** Like {@link #respond}, but for headers that legitimately repeat (e.g. Set-Cookie). */
    public FakeSiteFetcher respondWithHeaders(String uri, int status,
                                              Map<String, List<String>> headers, String body) {
        responses.put(URI.create(uri), new FetchResult(true, status, headers, body));
        return this;
    }

    public FakeSiteFetcher unreachable(String uri) {
        responses.put(URI.create(uri), FetchResult.unreachable());
        return this;
    }

    @Override
    public FetchResult fetch(URI uri) {
        // Unscripted URIs behave like a dead connection, the safe default.
        return responses.getOrDefault(uri, FetchResult.unreachable());
    }
}

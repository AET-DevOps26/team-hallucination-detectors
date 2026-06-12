package de.tum.devops.vibeshield.scanner.http;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Outcome of one HTTP probe. {@code reachable} is false when no HTTP response was
 * obtained at all (connection refused, TLS failure, timeout); checks interpret the
 * two cases very differently, so the distinction is explicit.
 */
public record FetchResult(
        boolean reachable,
        int status,
        Map<String, List<String>> headers,
        String bodySnippet
) {

    public static FetchResult unreachable() {
        return new FetchResult(false, -1, Map.of(), "");
    }

    /** First value of a header, case-insensitively (HTTP header names are case-insensitive). */
    public Optional<String> header(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(name))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst();
    }
}

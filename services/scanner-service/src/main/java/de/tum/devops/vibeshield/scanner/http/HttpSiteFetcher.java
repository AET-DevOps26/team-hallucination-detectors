package de.tum.devops.vibeshield.scanner.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production {@link SiteFetcher}: one instance per scan, enforcing the request
 * budget across all checks of that scan. Redirects are never followed implicitly —
 * checks that care about redirects inspect the 3xx response themselves.
 */
public class HttpSiteFetcher implements SiteFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpSiteFetcher.class);

    /** Upper bound on captured body bytes; heuristics only ever need the beginning. */
    private static final int MAX_BODY_BYTES = 16 * 1024;

    private final HttpClient client;
    private final Duration requestTimeout;
    private final String userAgent;
    private final int requestBudget;
    private final AtomicInteger requestsSent = new AtomicInteger();

    public HttpSiteFetcher(Duration connectTimeout, Duration requestTimeout,
                           String userAgent, int requestBudget) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.requestTimeout = requestTimeout;
        this.userAgent = userAgent;
        this.requestBudget = requestBudget;
    }

    @Override
    public FetchResult fetch(URI uri) {
        if (requestsSent.incrementAndGet() > requestBudget) {
            throw new RequestBudgetExceededException(requestBudget);
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("User-Agent", userAgent)
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String snippet = readSnippet(response.body());
            return new FetchResult(true, response.statusCode(), response.headers().map(), snippet);
        } catch (IOException exception) {
            log.debug("Fetch failed for {}: {}", uri, exception.toString());
            return FetchResult.unreachable();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return FetchResult.unreachable();
        }
    }

    private String readSnippet(InputStream body) {
        try (body) {
            return new String(body.readNBytes(MAX_BODY_BYTES), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }
}

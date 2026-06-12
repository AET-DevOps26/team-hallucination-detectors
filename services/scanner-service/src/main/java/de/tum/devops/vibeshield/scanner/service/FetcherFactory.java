package de.tum.devops.vibeshield.scanner.service;

import de.tum.devops.vibeshield.scanner.http.HttpSiteFetcher;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import de.tum.devops.vibeshield.scanner.http.SsrfGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Creates one {@link SiteFetcher} per scan so the request budget is scoped to a
 * single scan, not the service lifetime. Limits are configuration, not code.
 */
@Component
public class FetcherFactory {

    private final long timeoutMs;
    private final String userAgent;
    private final int requestBudget;
    private final SsrfGuard ssrfGuard;

    public FetcherFactory(@Value("${scanner.timeout-ms}") long timeoutMs,
                          @Value("${scanner.user-agent}") String userAgent,
                          @Value("${scanner.request-budget}") int requestBudget,
                          SsrfGuard ssrfGuard) {
        this.timeoutMs = timeoutMs;
        this.userAgent = userAgent;
        this.requestBudget = requestBudget;
        this.ssrfGuard = ssrfGuard;
    }

    public SiteFetcher newFetcher() {
        return new HttpSiteFetcher(Duration.ofMillis(timeoutMs), Duration.ofMillis(timeoutMs),
                userAgent, requestBudget, ssrfGuard);
    }
}

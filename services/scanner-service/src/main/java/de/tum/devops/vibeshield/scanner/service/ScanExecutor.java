package de.tum.devops.vibeshield.scanner.service;

import de.tum.devops.vibeshield.scanner.checks.SecurityCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionRequest;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionResult;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.http.BlockedAddressException;
import de.tum.devops.vibeshield.scanner.http.RequestBudgetExceededException;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the implemented checks against one target and assembles the contract result.
 * Unimplemented check types (secrets — post-MVP) are skipped and the
 * result's {@code executedChecks} makes that visible instead of silently lying.
 */
@Service
public class ScanExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScanExecutor.class);

    private final List<SecurityCheck> checks;
    private final FetcherFactory fetcherFactory;
    private final PageDiscovery pageDiscovery;
    private final MeterRegistry meterRegistry;

    public ScanExecutor(List<SecurityCheck> checks, FetcherFactory fetcherFactory,
                        PageDiscovery pageDiscovery, MeterRegistry meterRegistry) {
        this.checks = checks;
        this.fetcherFactory = fetcherFactory;
        this.pageDiscovery = pageDiscovery;
        this.meterRegistry = meterRegistry;
    }

    public ScanExecutionResult execute(ScanExecutionRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ScanExecutionResult result = doExecute(request);
        sample.stop(meterRegistry.timer("vibeshield_scan_duration_seconds",
                "status", result.getStatus().getValue()));
        meterRegistry.counter("vibeshield_scans_total", "status", result.getStatus().getValue()).increment();
        for (ScannerFinding finding : result.getFindings()) {
            meterRegistry.counter("vibeshield_findings_total",
                    "severity", finding.getSeverity().getValue()).increment();
        }
        return result;
    }

    private ScanExecutionResult doExecute(ScanExecutionRequest request) {
        URI target = request.getUrl();
        SiteFetcher fetcher = fetcherFactory.newFetcher();

        // One reachability probe up front, which also trips the SSRF guard: a target
        // pointing at a non-public address (loopback/private/link-local/internal) is a
        // Failed scan with a clear reason, never an attempted fetch. An unreachable
        // public site is likewise Failed-with-reason, not Completed-with-zero-findings.
        try {
            if (!fetcher.fetch(target).reachable()) {
                return failed("The site could not be reached at " + target + ".");
            }
        } catch (BlockedAddressException blocked) {
            log.warn("Blocked scan of {}: {}", target, blocked.getMessage());
            return failed(blocked.getMessage());
        }

        List<ScanCheck> executed = new ArrayList<>();
        List<URI> pages = List.of(target);
        if (request.getChecks().contains(ScanCheck.CRAWL)) {
            // crawlDepth 0 (the contract default) means "scan only the start URL" —
            // honor that literally rather than discovering pages nobody asked for.
            // Any depth >= 1 currently gets the same one-hop discovery; true
            // multi-hop crawling (depth 2/3) and the includeSubdomains option are
            // not yet honored (same-origin, one hop only).
            int crawlDepth = request.getCrawlDepth() == null ? 0 : request.getCrawlDepth();
            if (crawlDepth >= 1) {
                try {
                    pages = pageDiscovery.discover(target, fetcher);
                } catch (BlockedAddressException blocked) {
                    // A discovered link resolving to a non-public address mid-crawl
                    // (e.g. DNS rebinding) must not abort the scan; the target itself
                    // already passed the SSRF guard, so keep scanning the start URL.
                    log.warn("Blocked page discovery for {}: {}", target, blocked.getMessage());
                }
            }
            executed.add(ScanCheck.CRAWL);
        }

        List<ScannerFinding> findings = new ArrayList<>();
        for (SecurityCheck check : checks) {
            if (!request.getChecks().contains(check.type())) {
                continue;
            }
            try {
                findings.addAll(check.run(target, fetcher));
                executed.add(check.type());
                meterRegistry.counter("vibeshield_checks_run_total", "check", check.type().getValue()).increment();
            } catch (RequestBudgetExceededException exception) {
                log.warn("Request budget exhausted while scanning {}; stopping early", target);
                break;
            } catch (RuntimeException exception) {
                // One broken check must not void the rest of the scan.
                log.error("Check {} failed against {}", check.type(), target, exception);
            }
        }

        return new ScanExecutionResult()
                .status(ScanExecutionResult.StatusEnum.COMPLETED)
                .executedChecks(executed)
                .pages(pages)
                .findings(findings);
    }

    private static ScanExecutionResult failed(String errorMessage) {
        return new ScanExecutionResult()
                .status(ScanExecutionResult.StatusEnum.FAILED)
                .executedChecks(List.of())
                .pages(List.of())
                .findings(List.of())
                .errorMessage(errorMessage);
    }
}

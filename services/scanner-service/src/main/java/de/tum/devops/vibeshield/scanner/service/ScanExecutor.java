package de.tum.devops.vibeshield.scanner.service;

import de.tum.devops.vibeshield.scanner.checks.SecurityCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionRequest;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionResult;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.http.RequestBudgetExceededException;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the implemented checks against one target and assembles the contract result.
 * Unimplemented check types (crawl, adminPaths, secrets — post-MVP) are skipped and
 * the result's {@code executedChecks} makes that visible instead of silently lying.
 */
@Service
public class ScanExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScanExecutor.class);

    private final List<SecurityCheck> checks;
    private final FetcherFactory fetcherFactory;

    public ScanExecutor(List<SecurityCheck> checks, FetcherFactory fetcherFactory) {
        this.checks = checks;
        this.fetcherFactory = fetcherFactory;
    }

    public ScanExecutionResult execute(ScanExecutionRequest request) {
        URI target = request.getUrl();
        SiteFetcher fetcher = fetcherFactory.newFetcher();

        // One reachability probe up front: a completely unreachable site is a Failed
        // scan with a reason, not a Completed scan with zero findings.
        if (!fetcher.fetch(target).reachable()) {
            return new ScanExecutionResult()
                    .status(ScanExecutionResult.StatusEnum.FAILED)
                    .executedChecks(List.of())
                    .pages(List.of())
                    .findings(List.of())
                    .errorMessage("The site could not be reached at " + target + ".");
        }

        List<ScannerFinding> findings = new ArrayList<>();
        List<ScanCheck> executed = new ArrayList<>();
        for (SecurityCheck check : checks) {
            if (!request.getChecks().contains(check.type())) {
                continue;
            }
            try {
                findings.addAll(check.run(target, fetcher));
                executed.add(check.type());
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
                .pages(List.of(target))
                .findings(findings);
    }
}

package de.tum.devops.vibeshield.scanner;

import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.scannerclient.model.ScanCheck;
import de.tum.devops.vibeshield.scannerclient.model.ScanExecutionRequest;
import de.tum.devops.vibeshield.scannerclient.model.ScanExecutionResult;
import de.tum.devops.vibeshield.service.ScanProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.Optional;

/**
 * Background executor of the scan slice (issue #19): claims Pending scans from the
 * database (which doubles as the work queue, so scans survive restarts), sends them
 * to the scanner-service, and persists the outcome. The user-facing trigger stays a
 * fast 202 — all slow work happens here.
 *
 * <p>Disabled via {@code scan.worker.enabled=false} (tests drive it manually).
 */
@Component
@ConditionalOnProperty(name = "scan.worker.enabled", havingValue = "true", matchIfMissing = true)
public class ScanWorker {

    private static final Logger log = LoggerFactory.getLogger(ScanWorker.class);

    private final ScanProcessingService processingService;
    private final ScannerClient scannerClient;
    private final WebsiteRepository websiteRepository;

    public ScanWorker(ScanProcessingService processingService, ScannerClient scannerClient,
                      WebsiteRepository websiteRepository) {
        this.processingService = processingService;
        this.scannerClient = scannerClient;
        this.websiteRepository = websiteRepository;
    }

    @Scheduled(fixedDelayString = "${scan.worker.poll-interval-ms:2000}")
    public void processNextPendingScan() {
        Optional<Scan> pending = processingService.nextPending();
        if (pending.isEmpty()) {
            return;
        }

        // Graceful degradation (#28): scanner down → scans simply stay Pending and
        // are retried on a later tick instead of failing the user's scan.
        if (!scannerClient.isHealthy()) {
            log.warn("Scanner service is unavailable; leaving scan {} pending", pending.get().getId());
            return;
        }

        Scan scan = pending.get();
        if (!processingService.claim(scan.getId())) {
            return; // Another worker instance claimed it first.
        }

        try {
            ScanExecutionResult result = scannerClient.execute(toExecutionRequest(scan));
            if (result.getStatus() == ScanExecutionResult.StatusEnum.COMPLETED) {
                processingService.complete(scan.getId(), result.getFindings());
                log.info("Scan {} completed with {} findings", scan.getId(), result.getFindings().size());
            } else {
                processingService.fail(scan.getId(), result.getErrorMessage());
                log.info("Scan {} failed: {}", scan.getId(), result.getErrorMessage());
            }
        } catch (RestClientException exception) {
            processingService.fail(scan.getId(),
                    "The scanner service could not be reached. Please try again.");
            log.error("Scanner call failed for scan {}", scan.getId(), exception);
        } catch (RuntimeException exception) {
            processingService.fail(scan.getId(), "Unexpected error while processing the scan.");
            log.error("Unexpected error processing scan {}", scan.getId(), exception);
        }
    }

    private ScanExecutionRequest toExecutionRequest(Scan scan) {
        Website website = websiteRepository.findById(scan.getWebsiteId()).orElseThrow();
        return new ScanExecutionRequest()
                .url(URI.create(website.getUrl()))
                // Both enums are generated from the same contract values.
                .checks(scan.getRequestedChecks().stream()
                        .map(check -> ScanCheck.valueOf(check.name()))
                        .toList())
                .crawlDepth(scan.getCrawlDepth())
                .includeSubdomains(scan.isIncludeSubdomains());
    }
}

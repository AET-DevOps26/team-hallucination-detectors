package de.tum.devops.vibeshield.scanner;

import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.scannerclient.model.ScanExecutionResult;
import de.tum.devops.vibeshield.scannerclient.model.ScannerFinding;
import de.tum.devops.vibeshield.service.ScanProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Worker behaviour against the real persistence stack (H2 + Flyway), with only the
 * HTTP boundary to the scanner mocked: completion persists findings, failures store
 * reasons, an unavailable scanner leaves work Pending, and claims are race-safe.
 */
@SpringBootTest(properties = "app.jwt.secret=test-secret-that-is-at-least-32-chars!!")
@ActiveProfiles("test")
class ScanWorkerIT {

    @Autowired
    private ScanProcessingService processingService;
    @Autowired
    private WebsiteRepository websiteRepository;
    @Autowired
    private ScanRepository scanRepository;
    @Autowired
    private FindingRepository findingRepository;

    private ScannerClient scannerClient;
    private ScanWorker worker;

    @BeforeEach
    void setUp() {
        findingRepository.deleteAll();
        scanRepository.deleteAll();
        websiteRepository.deleteAll();
        scannerClient = mock(ScannerClient.class);
        worker = new ScanWorker(processingService, scannerClient, websiteRepository, 300_000L);
    }

    private Scan pendingScan() {
        Website website = websiteRepository.save(
                new Website(7L, "https://shop.example.org", "Shop", Instant.now()));
        return scanRepository.save(new Scan(website.getId(),
                List.of(ScanCheck.HTTPS, ScanCheck.HEADERS), 0, false, Instant.now()));
    }

    @Test
    void completedScan_persistsFindingsAndMarksCompleted() {
        Scan scan = pendingScan();
        when(scannerClient.isHealthy()).thenReturn(true);
        when(scannerClient.execute(any())).thenReturn(new ScanExecutionResult()
                .status(ScanExecutionResult.StatusEnum.COMPLETED)
                .executedChecks(List.of(de.tum.devops.vibeshield.scannerclient.model.ScanCheck.HEADERS))
                .pages(List.of(URI.create("https://shop.example.org")))
                .findings(List.of(new ScannerFinding()
                        .check(de.tum.devops.vibeshield.scannerclient.model.ScanCheck.HEADERS)
                        .title("Missing Content-Security-Policy header")
                        .severity(de.tum.devops.vibeshield.scannerclient.model.Severity.MEDIUM)
                        .affected("https://shop.example.org/")
                        .explanation("Scripts that sneak into the page run unrestricted.")
                        .suggestedFix("Add a Content-Security-Policy header."))));

        worker.processNextPendingScan();

        Scan processed = scanRepository.findById(scan.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(ScanStatus.COMPLETED);
        assertThat(processed.getCompletedAt()).isNotNull();

        List<Finding> findings = findingRepository.findAllByScanId(scan.getId());
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(findings.get(0).getCheckType()).isEqualTo(ScanCheck.HEADERS);
        assertThat(findings.get(0).getStatus()).isEqualTo(FindingStatus.OPEN);
    }

    @Test
    void scannerReportedFailure_marksScanFailedWithReason() {
        Scan scan = pendingScan();
        when(scannerClient.isHealthy()).thenReturn(true);
        when(scannerClient.execute(any())).thenReturn(new ScanExecutionResult()
                .status(ScanExecutionResult.StatusEnum.FAILED)
                .executedChecks(List.of())
                .pages(List.of())
                .findings(List.of())
                .errorMessage("The site could not be reached at https://shop.example.org."));

        worker.processNextPendingScan();

        Scan processed = scanRepository.findById(scan.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(ScanStatus.FAILED);
        assertThat(processed.getErrorMessage()).contains("could not be reached");
    }

    @Test
    void unreachableScanner_marksScanFailedGracefully() {
        Scan scan = pendingScan();
        when(scannerClient.isHealthy()).thenReturn(true);
        when(scannerClient.execute(any()))
                .thenThrow(new ResourceAccessException("connection refused"));

        worker.processNextPendingScan();

        Scan processed = scanRepository.findById(scan.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(ScanStatus.FAILED);
        assertThat(processed.getErrorMessage()).contains("scanner service");
    }

    @Test
    void unhealthyScanner_leavesScanPendingForRetry() {
        Scan scan = pendingScan();
        when(scannerClient.isHealthy()).thenReturn(false);

        worker.processNextPendingScan();

        Scan untouched = scanRepository.findById(scan.getId()).orElseThrow();
        assertThat(untouched.getStatus()).isEqualTo(ScanStatus.PENDING);
        verify(scannerClient, never()).execute(any());
    }

    @Test
    void claim_isWonExactlyOnce_andStampsStartedAt() {
        Scan scan = pendingScan();

        assertThat(processingService.claim(scan.getId())).isTrue();
        assertThat(processingService.claim(scan.getId())).isFalse();
        Scan claimed = scanRepository.findById(scan.getId()).orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo(ScanStatus.RUNNING);
        assertThat(claimed.getStartedAt()).isNotNull();
    }

    @Test
    void staleRunningScan_isRecoveredAsFailedWithReason() {
        Scan scan = pendingScan();
        assertThat(processingService.claim(scan.getId())).isTrue();

        // Cutoff in the future → the claimed scan counts as stuck past its timeout.
        int recovered = processingService.failStaleRunning(Instant.now().plusSeconds(60));

        assertThat(recovered).isEqualTo(1);
        Scan processed = scanRepository.findById(scan.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(ScanStatus.FAILED);
        assertThat(processed.getErrorMessage()).contains("did not finish in time");
        assertThat(processed.getCompletedAt()).isNotNull();
    }

    @Test
    void freshlyClaimedScan_isWithinTimeout_andNotRecovered() {
        Scan scan = pendingScan();
        assertThat(processingService.claim(scan.getId())).isTrue();

        // Cutoff in the past → the just-claimed scan is still within its timeout.
        int recovered = processingService.failStaleRunning(Instant.now().minusSeconds(60));

        assertThat(recovered).isZero();
        assertThat(scanRepository.findById(scan.getId()).orElseThrow().getStatus())
                .isEqualTo(ScanStatus.RUNNING);
    }

    @Test
    void noPendingScans_isANoOp() {
        worker.processNextPendingScan();

        verify(scannerClient, never()).isHealthy();
        verify(scannerClient, never()).execute(any());
    }
}

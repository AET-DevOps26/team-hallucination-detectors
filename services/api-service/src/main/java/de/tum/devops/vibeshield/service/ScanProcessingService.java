package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.scannerclient.model.ScannerFinding;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The worker's state transitions, each in its own short transaction so no
 * transaction is ever held open across the HTTP call to the scanner.
 */
@Service
public class ScanProcessingService {

    private final ScanRepository scanRepository;
    private final FindingRepository findingRepository;

    public ScanProcessingService(ScanRepository scanRepository, FindingRepository findingRepository) {
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
    }

    /** The oldest Pending scan, if any — the worker's work queue is the scans table. */
    @Transactional(readOnly = true)
    public Optional<Scan> nextPending() {
        return scanRepository.findFirstByStatusOrderByCreatedAtAscIdAsc(ScanStatus.PENDING);
    }

    /**
     * Atomically claims a scan (Pending → Running). Returns false when another
     * worker instance won the race — the status guard in the update is the lock.
     */
    @Transactional
    public boolean claim(Long scanId) {
        return scanRepository.transitionStatus(scanId, ScanStatus.PENDING, ScanStatus.RUNNING) == 1;
    }

    /** Persists the findings and completes the scan (issues #19, #20). */
    @Transactional
    public void complete(Long scanId, List<ScannerFinding> scannerFindings) {
        List<Finding> findings = scannerFindings.stream()
                .map(finding -> new Finding(
                        scanId,
                        // Both enums are generated from the same contract values.
                        ScanCheck.valueOf(finding.getCheck().name()),
                        finding.getTitle(),
                        Severity.valueOf(finding.getSeverity().name()),
                        finding.getAffected(),
                        finding.getExplanation(),
                        finding.getSuggestedFix()))
                .toList();
        findingRepository.saveAll(findings);

        Scan scan = scanRepository.findById(scanId).orElseThrow();
        scan.markCompleted(Instant.now());
        scanRepository.save(scan);
    }

    /** Marks the scan Failed and stores the reason for the user (issue #19). */
    @Transactional
    public void fail(Long scanId, String errorMessage) {
        Scan scan = scanRepository.findById(scanId).orElseThrow();
        scan.markFailed(Instant.now(), errorMessage);
        scanRepository.save(scan);
    }
}

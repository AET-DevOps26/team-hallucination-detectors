package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.ConflictException;
import de.tum.devops.vibeshield.exception.NotFoundException;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.ScanRequest;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Scan lifecycle from the user's perspective (issues #16, #18, #21): triggering,
 * status polling, history, and findings. Execution itself belongs to the background
 * worker. Every lookup resolves ownership via the website (scan → website → owner)
 * and answers 404 for anything the caller does not own.
 */
@Service
public class ScanService {

    /** At most one scan per website may be in one of these states (contract: 409). */
    private static final Set<ScanStatus> IN_FLIGHT = Set.of(ScanStatus.PENDING, ScanStatus.RUNNING);

    private final ScanRepository scanRepository;
    private final FindingRepository findingRepository;
    private final WebsiteRepository websiteRepository;

    public ScanService(ScanRepository scanRepository, FindingRepository findingRepository,
                       WebsiteRepository websiteRepository) {
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
        this.websiteRepository = websiteRepository;
    }

    /** Creates a Pending scan for an owned website (issue #16); the worker picks it up. */
    @Transactional
    public Scan trigger(AuthenticatedUser user, Long websiteId, ScanRequest request) {
        requireOwnedWebsite(user, websiteId);
        if (scanRepository.existsByWebsiteIdAndStatusIn(websiteId, IN_FLIGHT)) {
            throw new ConflictException("SCAN_IN_PROGRESS",
                    "A scan for this website is already pending or running.");
        }

        List<ScanCheck> checks = (request == null || request.getChecks() == null
                || request.getChecks().isEmpty())
                ? List.of(ScanCheck.values())
                : List.copyOf(request.getChecks());
        int crawlDepth = (request == null || request.getCrawlDepth() == null) ? 0 : request.getCrawlDepth();
        boolean includeSubdomains = request != null && Boolean.TRUE.equals(request.getIncludeSubdomains());

        return scanRepository.save(
                new Scan(websiteId, checks, crawlDepth, includeSubdomains, Instant.now()));
    }

    /** Current scan state for polling (issue #18). */
    @Transactional(readOnly = true)
    public Scan getScan(AuthenticatedUser user, Long scanId) {
        return requireOwnedScan(user, scanId);
    }

    /** Scan history of an owned website, newest first. */
    @Transactional(readOnly = true)
    public List<Scan> listScans(AuthenticatedUser user, Long websiteId) {
        requireOwnedWebsite(user, websiteId);
        return scanRepository.findAllByWebsiteIdOrderByCreatedAtDescIdDesc(websiteId);
    }

    /** Most recent scan of an owned website (issue #21); 404 when none exists yet. */
    @Transactional(readOnly = true)
    public Scan getLatestScan(AuthenticatedUser user, Long websiteId) {
        requireOwnedWebsite(user, websiteId);
        return scanRepository.findFirstByWebsiteIdOrderByCreatedAtDescIdDesc(websiteId)
                .orElseThrow(() -> new NotFoundException("SCAN_NOT_FOUND",
                        "This website has not been scanned yet."));
    }

    /** Findings of an owned scan, most severe first; empty until the scan completed. */
    @Transactional(readOnly = true)
    public List<Finding> listFindings(AuthenticatedUser user, Long scanId) {
        requireOwnedScan(user, scanId);
        return findingRepository.findAllByScanId(scanId).stream()
                .sorted(Comparator
                        .comparing((Finding finding) -> finding.getSeverity().ordinal())
                        .thenComparing(Finding::getId))
                .toList();
    }

    private void requireOwnedWebsite(AuthenticatedUser user, Long websiteId) {
        websiteRepository.findByIdAndOwnerId(websiteId, user.userId())
                .orElseThrow(() -> new NotFoundException("WEBSITE_NOT_FOUND", "Website not found."));
    }

    private Scan requireOwnedScan(AuthenticatedUser user, Long scanId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new NotFoundException("SCAN_NOT_FOUND", "Scan not found."));
        // Same 404 whether the scan is missing or foreign — IDs must not be enumerable.
        websiteRepository.findByIdAndOwnerId(scan.getWebsiteId(), user.userId())
                .orElseThrow(() -> new NotFoundException("SCAN_NOT_FOUND", "Scan not found."));
        return scan;
    }
}

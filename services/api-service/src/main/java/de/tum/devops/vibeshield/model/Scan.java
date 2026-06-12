package de.tum.devops.vibeshield.model;

import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

/**
 * A scan job and its lifecycle state (issue #17): Pending → Running → Completed/Failed.
 * Created Pending by the trigger endpoint; the background worker performs every later
 * transition. Status enums are the contract-generated types — one source of truth.
 */
@Entity
@Table(name = "scans")
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "website_id", nullable = false)
    private Long websiteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScanStatus status;

    @Convert(converter = ScanCheckListConverter.class)
    @Column(name = "requested_checks", nullable = false)
    private List<ScanCheck> requestedChecks;

    @Column(name = "crawl_depth", nullable = false)
    private int crawlDepth;

    @Column(name = "include_subdomains", nullable = false)
    private boolean includeSubdomains;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    protected Scan() {
        // JPA only
    }

    public Scan(Long websiteId, List<ScanCheck> requestedChecks, int crawlDepth,
                boolean includeSubdomains, Instant createdAt) {
        this.websiteId = websiteId;
        this.status = ScanStatus.PENDING;
        this.requestedChecks = requestedChecks;
        this.crawlDepth = crawlDepth;
        this.includeSubdomains = includeSubdomains;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getWebsiteId() {
        return websiteId;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public List<ScanCheck> getRequestedChecks() {
        return requestedChecks;
    }

    public int getCrawlDepth() {
        return crawlDepth;
    }

    public boolean isIncludeSubdomains() {
        return includeSubdomains;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Worker transition: the scan finished and its findings are persisted. */
    public void markCompleted(Instant completedAt) {
        this.status = ScanStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    /** Worker transition: the scan could not be executed; the reason is kept for the user. */
    public void markFailed(Instant completedAt, String errorMessage) {
        this.status = ScanStatus.FAILED;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
    }
}

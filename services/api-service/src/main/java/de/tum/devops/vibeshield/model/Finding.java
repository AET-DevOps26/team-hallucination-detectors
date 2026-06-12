package de.tum.devops.vibeshield.model;

import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single security issue discovered by a scan (issue #20). Written once by the
 * background worker after scan completion; new findings always start {@code Open}.
 */
@Entity
@Table(name = "findings")
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scan_id", nullable = false)
    private Long scanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 32)
    private ScanCheck checkType;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(nullable = false, length = 2048)
    private String affected;

    @Column(nullable = false, length = 4000)
    private String explanation;

    @Column(name = "suggested_fix", nullable = false, length = 4000)
    private String suggestedFix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FindingStatus status;

    protected Finding() {
        // JPA only
    }

    public Finding(Long scanId, ScanCheck checkType, String title, Severity severity,
                   String affected, String explanation, String suggestedFix) {
        this.scanId = scanId;
        this.checkType = checkType;
        this.title = title;
        this.severity = severity;
        this.affected = affected;
        this.explanation = explanation;
        this.suggestedFix = suggestedFix;
        this.status = FindingStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public Long getScanId() {
        return scanId;
    }

    public ScanCheck getCheckType() {
        return checkType;
    }

    public String getTitle() {
        return title;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getAffected() {
        return affected;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public FindingStatus getStatus() {
        return status;
    }
}

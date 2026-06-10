package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.model.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence for scan jobs. Ownership is enforced one level up (scan → website →
 * owner), so callers must resolve the website through an owner-scoped query first.
 */
public interface ScanRepository extends JpaRepository<Scan, Long> {

    List<Scan> findAllByWebsiteIdOrderByCreatedAtDescIdDesc(Long websiteId);

    Optional<Scan> findFirstByWebsiteIdOrderByCreatedAtDescIdDesc(Long websiteId);

    boolean existsByWebsiteIdAndStatusIn(Long websiteId, Collection<ScanStatus> statuses);

    Optional<Scan> findFirstByStatusOrderByCreatedAtAscIdAsc(ScanStatus status);

    /**
     * Guarded status transition: only applies when the scan is still in
     * {@code expected}. The returned row count (0 or 1) tells the caller whether
     * it won the claim — this is the worker's concurrency control.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Scan s SET s.status = :next WHERE s.id = :id AND s.status = :expected")
    int transitionStatus(@Param("id") Long id,
                         @Param("expected") ScanStatus expected,
                         @Param("next") ScanStatus next);
}

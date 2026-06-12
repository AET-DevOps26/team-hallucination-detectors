package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.model.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
     * Guarded claim (Pending → Running) that also stamps when the scan was claimed.
     * The status guard is the lock: a returned row count of 1 means this worker won
     * the claim, 0 means another instance got there first. The timestamp lets
     * {@link #failStaleRunning} recover scans whose worker died mid-run.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Scan s SET s.status = :next, s.startedAt = :startedAt "
            + "WHERE s.id = :id AND s.status = :expected")
    int claim(@Param("id") Long id,
              @Param("expected") ScanStatus expected,
              @Param("next") ScanStatus next,
              @Param("startedAt") Instant startedAt);

    /**
     * Recovery sweep: fail every scan still Running since before {@code cutoff}. Such
     * a scan was claimed by a worker that then died before completing it — the table
     * is the queue and only Pending scans are picked up, so it would stay Running
     * forever. Guarded on status so it can never clobber a scan another worker is
     * finishing right now. Returns the number recovered.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Scan s SET s.status = :failed, s.completedAt = :now, s.errorMessage = :message "
            + "WHERE s.status = :running AND s.startedAt < :cutoff")
    int failStaleRunning(@Param("running") ScanStatus running,
                         @Param("failed") ScanStatus failed,
                         @Param("now") Instant now,
                         @Param("cutoff") Instant cutoff,
                         @Param("message") String message);
}

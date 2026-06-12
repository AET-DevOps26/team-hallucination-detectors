package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.model.Scan;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

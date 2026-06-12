package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for registered websites. Every query is owner-scoped — callers must
 * never load a website without the owner filter.
 */
public interface WebsiteRepository extends JpaRepository<Website, Long> {

    List<Website> findAllByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    Optional<Website> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndUrl(Long ownerId, String url);
}

package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.model.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Persistence for scan findings; access is always scoped through an owned scan. */
public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findAllByScanId(Long scanId);
}

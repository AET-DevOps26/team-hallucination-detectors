package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /** Looks up a user by email; used for login and duplicate-registration checks. */
    Optional<User> findByEmail(String email);
}
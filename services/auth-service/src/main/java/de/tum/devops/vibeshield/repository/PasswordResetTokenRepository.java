package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PasswordResetToken} entities.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    /** Looks up a token by its opaque value; used to redeem a reset request. */
    Optional<PasswordResetToken> findByToken(String token);

    /** All tokens previously issued to a user; used to invalidate them when a new one is requested. */
    List<PasswordResetToken> findByUserId(Long userId);
}

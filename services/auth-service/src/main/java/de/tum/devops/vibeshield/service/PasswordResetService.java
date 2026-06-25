package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.model.PasswordResetToken;
import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.PasswordResetTokenRepository;
import de.tum.devops.vibeshield.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and redeems single-use password reset tokens.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Issues a fresh reset token for the user, invalidating any earlier ones so only the
     * latest request is redeemable.
     *
     * <p>No email provider is wired up yet (tracked in docs/auth.md known gaps), so the link
     * is logged rather than emailed — the token mechanics are real, only the delivery channel
     * is a placeholder.
     */
    @Transactional
    public void requestReset(User user) {
        tokenRepository.deleteAll(tokenRepository.findByUserId(user.getId()));

        String token = generateToken();
        tokenRepository.save(new PasswordResetToken(user.getId(), token, Instant.now().plus(TOKEN_TTL)));

        log.info("Password reset requested for {}. No email provider is configured (see docs/auth.md); "
                + "reset token: {}", user.getEmail(), token);
    }

    /**
     * Redeems a reset token: if it is unknown, expired, or already used, returns false and
     * leaves the user's password untouched. Otherwise updates the password and consumes the
     * token so it cannot be redeemed again.
     */
    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        Optional<PasswordResetToken> maybeToken = tokenRepository.findByToken(rawToken)
                .filter(token -> token.isValid(Instant.now()));
        if (maybeToken.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = maybeToken.get();
        User user = userRepository.findById(resetToken.getUserId()).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);
        return true;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

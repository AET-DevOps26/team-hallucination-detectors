package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.model.PasswordResetToken;
import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.PasswordResetTokenRepository;
import de.tum.devops.vibeshield.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private PasswordResetTokenRepository tokenRepository;
    private UserRepository userRepository;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(PasswordResetTokenRepository.class);
        userRepository = mock(UserRepository.class);
        service = new PasswordResetService(tokenRepository, userRepository);
    }

    private User user(long id, String email) {
        User u = new User(email, "hashed");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private PasswordResetToken validToken(long userId, String token) {
        return new PasswordResetToken(userId, token, Instant.now().plusSeconds(3600));
    }

    private PasswordResetToken expiredToken(long userId, String token) {
        return new PasswordResetToken(userId, token, Instant.now().minusSeconds(1));
    }

    // ── requestReset ──────────────────────────────────────────────────────────

    @Test
    void requestReset_deletesOldTokensAndSavesNew() {
        User u = user(1L, "a@b.com");
        when(tokenRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        service.requestReset(u);

        verify(tokenRepository).deleteAll(any());
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void requestReset_generatesUnpredictableToken() {
        User u = user(2L, "b@c.com");
        when(tokenRepository.findByUserId(2L)).thenReturn(Collections.emptyList());

        ArgumentCaptor<PasswordResetToken> c1 = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<PasswordResetToken> c2 = ArgumentCaptor.forClass(PasswordResetToken.class);

        service.requestReset(u);
        verify(tokenRepository).save(c1.capture());
        reset(tokenRepository);
        when(tokenRepository.findByUserId(2L)).thenReturn(Collections.emptyList());
        service.requestReset(u);
        verify(tokenRepository).save(c2.capture());

        assertThat(c1.getValue().getToken()).isNotEqualTo(c2.getValue().getToken());
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_returnsTrueAndUpdatesPassword() {
        User u = user(1L, "a@b.com");
        PasswordResetToken token = validToken(1L, "good-token");
        when(tokenRepository.findByToken("good-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        boolean result = service.resetPassword("good-token", "NewPassword1!");

        assertThat(result).isTrue();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertThat(encoder.matches("NewPassword1!", u.getPassword())).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void resetPassword_returnsFalseForUnknownToken() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        boolean result = service.resetPassword("bad-token", "anything");

        assertThat(result).isFalse();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void resetPassword_returnsFalseForExpiredToken() {
        PasswordResetToken token = expiredToken(1L, "expired-token");
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        boolean result = service.resetPassword("expired-token", "anything");

        assertThat(result).isFalse();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void resetPassword_returnsFalseForAlreadyUsedToken() {
        PasswordResetToken token = validToken(1L, "used-token");
        token.setUsedAt(Instant.now().minusSeconds(60));
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        boolean result = service.resetPassword("used-token", "anything");

        assertThat(result).isFalse();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void resetPassword_marksTokenAsUsedSoItCannotBeRedeemedTwice() {
        User u = user(1L, "a@b.com");
        PasswordResetToken token = validToken(1L, "one-time-token");
        when(tokenRepository.findByToken("one-time-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        service.resetPassword("one-time-token", "NewPassword1!");

        assertThat(token.getUsedAt()).isNotNull();
        // Second attempt: token now has usedAt set, isValid() returns false
        assertThat(token.isValid(Instant.now())).isFalse();
    }
}
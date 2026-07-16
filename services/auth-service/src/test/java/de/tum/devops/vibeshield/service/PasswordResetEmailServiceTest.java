package de.tum.devops.vibeshield.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasswordResetEmailServiceTest {

    @Test
    void sendsEncodedResetLinkToUser() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        PasswordResetEmailService service = new PasswordResetEmailService(
                mailSender, "no-reply@vibeshield.test", "https://vibeshield.test/reset-password");

        service.sendResetLink("user@example.com", "base64url_token-with-safe-chars");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@vibeshield.test");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).contains("Reset");
        assertThat(message.getText()).contains(
                "https://vibeshield.test/reset-password?token=base64url_token-with-safe-chars");
    }
}

package de.tum.devops.vibeshield.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetEmailServiceTest {

    @Test
    void sendsClickableHtmlButtonAndPlainTextFallback() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        PasswordResetEmailService service = new PasswordResetEmailService(
                mailSender, "no-reply@vibeshield.test", "https://vibeshield.test/reset-password");

        service.sendResetLink("user@example.com", "base64url_token-with-safe-chars");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        assertThat(message.getFrom()[0].toString()).isEqualTo("no-reply@vibeshield.test");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        assertThat(message.getSubject()).contains("Reset");

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        message.writeTo(rawMessage);
        String content = rawMessage.toString(StandardCharsets.UTF_8);
        String resetLink = "https://vibeshield.test/reset-password?token=base64url_token-with-safe-chars";

        assertThat(content)
                .contains("Content-Type: text/plain")
                .contains("Content-Type: text/html")
                .contains("This link expires in one hour")
                .contains("VibeShield account security")
                .contains("Reset password")
                .contains("href=\"" + resetLink + "\"")
                .contains(resetLink)
                .doesNotContain("confidential")
                .doesNotContain("privileged information");
    }
}

package de.tum.devops.vibeshield.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * Delivers password-reset links through the configured SMTP server.
 */
@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String resetPageUrl;

    public PasswordResetEmailService(
            JavaMailSender mailSender,
            @Value("${app.password-reset.mail-from}") String from,
            @Value("${app.password-reset.page-url}") String resetPageUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.resetPageUrl = resetPageUrl;
    }

    public void sendResetLink(String recipient, String token) {
        String encodedToken = UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8);
        String resetLink = UriComponentsBuilder.fromUriString(resetPageUrl)
                .queryParam("token", encodedToken)
                .build(true)
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Reset your VibeShield password");
        message.setText("""
                We received a request to reset your VibeShield password.

                Use this link within one hour:
                %s

                If you did not request this change, you can ignore this email.
                """.formatted(resetLink));
        mailSender.send(message);
    }
}

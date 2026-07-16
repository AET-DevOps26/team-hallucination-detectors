package de.tum.devops.vibeshield.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

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
        String resetLink = UriComponentsBuilder.fromUriString(resetPageUrl)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();

        String plainText = """
                Reset your VibeShield password

                Use the link below to choose a new password. This link expires in one hour.
                %s

                If you did not request a password reset, no action is required.

                VibeShield
                """.formatted(resetLink);
        String safeResetLink = HtmlUtils.htmlEscape(resetLink);
        String html = """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Reset your VibeShield password</title>
                  </head>
                  <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,Helvetica,sans-serif;color:#18181b">
                    <div style="display:none;max-height:0;overflow:hidden;opacity:0">
                      Choose a new VibeShield password. This link expires in one hour.
                    </div>
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f4f4f5">
                      <tr>
                        <td align="center" style="padding:40px 16px">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px">
                            <tr>
                              <td style="padding:0 0 18px;text-align:center;font-size:22px;font-weight:700;letter-spacing:-0.3px">
                                VibeShield
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:36px;background:#ffffff;border:1px solid #e4e4e7;border-radius:14px;box-shadow:0 8px 24px rgba(24,24,27,0.06)">
                                <h1 style="margin:0 0 14px;font-size:25px;line-height:1.25;letter-spacing:-0.4px">
                                  Reset your password
                                </h1>
                                <p style="margin:0 0 26px;color:#52525b;font-size:15px;line-height:1.6">
                                  Use the button below to choose a new password for your VibeShield account.
                                  This link expires in one hour.
                                </p>
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                                  <tr>
                                    <td style="border-radius:8px;background:#18181b">
                                      <a href="%s" style="display:inline-block;padding:13px 22px;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700">
                                        Reset password
                                      </a>
                                    </td>
                                  </tr>
                                </table>
                                <div style="height:28px"></div>
                                <p style="margin:0 0 6px;color:#71717a;font-size:12px;line-height:1.5">
                                  If the button does not work, copy this link into your browser:
                                </p>
                                <p style="margin:0;font-size:12px;line-height:1.5;word-break:break-all">
                                  <a href="%s" style="color:#52525b;text-decoration:underline">%s</a>
                                </p>
                                <div style="height:28px"></div>
                                <div style="border-top:1px solid #e4e4e7;padding-top:20px">
                                  <p style="margin:0;color:#71717a;font-size:12px;line-height:1.6">
                                    If you did not request a password reset, no action is required.
                                  </p>
                                </div>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:18px 12px 0;text-align:center;color:#a1a1aa;font-size:11px">
                                VibeShield account security
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(safeResetLink, safeResetLink, safeResetLink);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("Reset your VibeShield password");
            helper.setText(plainText, html);
        } catch (MessagingException exception) {
            throw new MailPreparationException("Could not prepare password reset email", exception);
        }
        mailSender.send(message);
    }
}

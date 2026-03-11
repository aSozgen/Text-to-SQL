package com.texttosql.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email - QueryGen");

            String verificationLink = frontendUrl + "/verify-email?token=" + token;
            String htmlContent = buildVerificationEmail(verificationLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password - QueryGen");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetEmail(resetLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildVerificationEmail(String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0d1117;font-family:Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0d1117;padding:40px 20px;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background-color:#161b22;border:1px solid #30363d;border-radius:12px;overflow:hidden;max-width:600px;width:100%%;">

                            <!-- Header -->
                            <tr>
                                <td style="background:linear-gradient(135deg,#1f6feb,#388bfd);padding:32px 40px;text-align:center;">
                                    <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;letter-spacing:-0.5px;">
                                        ⚡ QueryGen
                                    </h1>
                                </td>
                            </tr>

                            <!-- Body -->
                            <tr>
                                <td style="padding:40px;">
                                    <h2 style="margin:0 0 16px 0;color:#e6edf3;font-size:20px;font-weight:600;">
                                        Verify Your Email Address
                                    </h2>
                                    <p style="margin:0 0 12px 0;color:#8b949e;font-size:15px;line-height:1.6;">
                                        Welcome to QueryGen! Please verify your email address to activate your account.
                                    </p>
                                    <p style="margin:0 0 32px 0;color:#8b949e;font-size:15px;line-height:1.6;">
                                        Click the button below to complete your registration:
                                    </p>

                                    <!-- Button -->
                                    <table cellpadding="0" cellspacing="0" width="100%%">
                                        <tr>
                                            <td align="center">
                                                <a href="%s"
                                                   style="display:inline-block;padding:14px 36px;background-color:#238636;color:#ffffff;
                                                          text-decoration:none;border-radius:8px;font-size:15px;font-weight:600;
                                                          letter-spacing:0.3px;border:1px solid #2ea043;">
                                                    ✓ Verify Email Address
                                                </a>
                                            </td>
                                        </tr>
                                    </table>

                                    <!-- Divider -->
                                    <table cellpadding="0" cellspacing="0" width="100%%" style="margin:32px 0;">
                                        <tr><td style="border-top:1px solid #21262d;"></td></tr>
                                    </table>

                                    <p style="margin:0 0 8px 0;color:#6e7681;font-size:13px;">
                                        Or copy and paste this link into your browser:
                                    </p>
                                    <p style="margin:0;word-break:break-all;">
                                        <a href="%s" style="color:#388bfd;font-size:13px;text-decoration:none;">%s</a>
                                    </p>

                                    <!-- Expiry notice -->
                                    <table cellpadding="0" cellspacing="0" width="100%%"
                                           style="margin-top:24px;background-color:#0d1117;border:1px solid #21262d;border-radius:8px;">
                                        <tr>
                                            <td style="padding:14px 18px;">
                                                <p style="margin:0;color:#8b949e;font-size:13px;">
                                                    🕐 This link will expire in <strong style="color:#e6edf3;">24 hours</strong>.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>

                            <!-- Footer -->
                            <tr>
                                <td style="padding:20px 40px;background-color:#010409;border-top:1px solid #21262d;">
                                    <p style="margin:0;color:#484f58;font-size:12px;text-align:center;">
                                        If you didn't create a QueryGen account, you can safely ignore this email.
                                    </p>
                                </td>
                            </tr>

                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(verificationLink, verificationLink, verificationLink);
    }

    private String buildPasswordResetEmail(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0d1117;font-family:Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0d1117;padding:40px 20px;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background-color:#161b22;border:1px solid #30363d;border-radius:12px;overflow:hidden;max-width:600px;width:100%%;">

                            <!-- Header -->
                            <tr>
                                <td style="background:linear-gradient(135deg,#6e40c9,#8957e5);padding:32px 40px;text-align:center;">
                                    <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;letter-spacing:-0.5px;">
                                        ⚡ QueryGen
                                    </h1>
                                </td>
                            </tr>

                            <!-- Body -->
                            <tr>
                                <td style="padding:40px;">
                                    <h2 style="margin:0 0 16px 0;color:#e6edf3;font-size:20px;font-weight:600;">
                                        Reset Your Password
                                    </h2>
                                    <p style="margin:0 0 12px 0;color:#8b949e;font-size:15px;line-height:1.6;">
                                        We received a request to reset your QueryGen account password.
                                    </p>
                                    <p style="margin:0 0 32px 0;color:#8b949e;font-size:15px;line-height:1.6;">
                                        Click the button below to choose a new password:
                                    </p>

                                    <!-- Button -->
                                    <table cellpadding="0" cellspacing="0" width="100%%">
                                        <tr>
                                            <td align="center">
                                                <a href="%s"
                                                   style="display:inline-block;padding:14px 36px;background-color:#8957e5;color:#ffffff;
                                                          text-decoration:none;border-radius:8px;font-size:15px;font-weight:600;
                                                          letter-spacing:0.3px;border:1px solid #a371f7;">
                                                    🔑 Reset Password
                                                </a>
                                            </td>
                                        </tr>
                                    </table>

                                    <!-- Divider -->
                                    <table cellpadding="0" cellspacing="0" width="100%%" style="margin:32px 0;">
                                        <tr><td style="border-top:1px solid #21262d;"></td></tr>
                                    </table>

                                    <p style="margin:0 0 8px 0;color:#6e7681;font-size:13px;">
                                        Or copy and paste this link into your browser:
                                    </p>
                                    <p style="margin:0;word-break:break-all;">
                                        <a href="%s" style="color:#a371f7;font-size:13px;text-decoration:none;">%s</a>
                                    </p>

                                    <!-- Expiry notice -->
                                    <table cellpadding="0" cellspacing="0" width="100%%"
                                           style="margin-top:24px;background-color:#0d1117;border:1px solid #21262d;border-radius:8px;">
                                        <tr>
                                            <td style="padding:14px 18px;">
                                                <p style="margin:0;color:#8b949e;font-size:13px;">
                                                    🕐 This link will expire in <strong style="color:#e6edf3;">1 hour</strong>.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>

                            <!-- Footer -->
                            <tr>
                                <td style="padding:20px 40px;background-color:#010409;border-top:1px solid #21262d;">
                                    <p style="margin:0;color:#484f58;font-size:12px;text-align:center;">
                                        If you didn't request a password reset, you can safely ignore this email.
                                        Your password will remain unchanged.
                                    </p>
                                </td>
                            </tr>

                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(resetLink, resetLink, resetLink);
    }
}

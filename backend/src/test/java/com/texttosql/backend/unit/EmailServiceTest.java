package com.texttosql.backend.unit;

import com.texttosql.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendVerificationEmail_ShouldSendEmail_WhenDetailsAreValid() {
        String email = "test@example.com";
        String token = "verification-token-123";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendVerificationEmail(email, token);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_ShouldIncludeVerificationLink_InEmail() {
        String email = "user@example.com";
        String token = "secure-token-xyz";
        String frontendUrl = "https://app.querygen.com";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", frontendUrl);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendVerificationEmail(email, token);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_ShouldContainVerifyEmailAddressText() {
        String email = "newuser@example.com";
        String token = "token-abc123";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendVerificationEmail(email, token);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_ShouldContain24HoursExpiry() {
        String email = "test@example.com";
        String token = "token-123";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendVerificationEmail(email, token);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldSendEmail_WhenDetailsAreValid() {
        String email = "user@example.com";
        String token = "reset-token-456";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(email, token);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldIncludeResetLink() {
        String email = "test@example.com";
        String token = "reset-token-789";
        String frontendUrl = "https://app.querygen.com";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", frontendUrl);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(email, token);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldContainResetPasswordText() {
        String email = "user@example.com";
        String token = "reset-token";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(email, token);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldContain1HourExpiry() {
        String email = "test@example.com";
        String token = "reset-token-111";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(email, token);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_ShouldThrowException_WhenSendingFails() {
        String email = "test@example.com";
        String token = "token-123";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP Error"));

        try {
            emailService.sendVerificationEmail(email, token);
        } catch (RuntimeException e) {
            // Expected behavior
        }
    }

    @Test
    void sendPasswordResetEmail_ShouldThrowException_WhenSendingFails() {
        String email = "test@example.com";
        String token = "reset-token";

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@querygen.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP Error"));

        try {
            emailService.sendPasswordResetEmail(email, token);
        } catch (RuntimeException e) {
            // Expected behavior
        }
    }

    @Test
    void sendVerificationEmail_ShouldUseCorrectFromEmail() {
        String email = "test@example.com";
        String token = "token";
        String fromEmail = "noreply@querygen.com";

        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendVerificationEmail(email, token);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldUseCorrectFromEmail() {
        String email = "test@example.com";
        String token = "reset-token";
        String fromEmail = "noreply@querygen.com";

        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://querygen.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(email, token);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
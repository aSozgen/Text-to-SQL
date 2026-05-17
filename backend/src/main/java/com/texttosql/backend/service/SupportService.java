package com.texttosql.backend.service;

import com.texttosql.backend.dto.support.SupportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SupportService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String adminEmail;

    public void sendSupportEmail(SupportRequest request) {
        log.info("Sending support email from: {}", request.getEmail());

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(request.getEmail());
        mailMessage.setTo(adminEmail);
        mailMessage.setSubject("Support Request: " + request.getSubject());
        mailMessage.setText("From: " + request.getEmail() + "\n\n" +
                           "Message:\n" + request.getMessage());

        try {
            mailSender.send(mailMessage);
            log.info("Support email sent successfully to {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send support email: {}", e.getMessage(), e);
            throw new RuntimeException("Could not send support email. Please try again later.");
        }
    }
}

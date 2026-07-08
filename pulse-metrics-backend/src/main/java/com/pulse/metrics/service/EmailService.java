package com.pulse.metrics.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:anmolkhanna1121@gmail.com}")
    private String fromAddress;

    // Use optional autowiring in case SMTP properties are not configured in application.yml
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmailWithAttachment(String to, String subject, String body, File attachment) {
        if (mailSender == null) {
            logMockEmail(to, subject, body, attachment);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);

            FileSystemResource file = new FileSystemResource(attachment);
            helper.addAttachment(attachment.getName(), file);

            mailSender.send(message);
            log.info("Email successfully sent to {} with attachment: {}", to, attachment.getName());

        } catch (Exception e) {
            log.warn("Failed to send real email via SMTP: {}", e.getMessage(), e);
            logMockEmail(to, subject, body, attachment);
        }
    }

    private void logMockEmail(String to, String subject, String body, File attachment) {
        log.info("\n" +
                "========================================================================\n" +
                "🌟 [MOCK EMAIL SENT SUCCESSFULY] 🌟\n" +
                "To:         {}\n" +
                "Subject:    {}\n" +
                "Body:       {}\n" +
                "Attachment: {} (Saved locally at: {})\n" +
                "========================================================================",
                to, subject, body, attachment.getName(), attachment.getAbsolutePath());
    }
}

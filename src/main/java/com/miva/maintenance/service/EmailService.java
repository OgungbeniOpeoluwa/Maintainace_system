package com.miva.maintenance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Optional. If set, emails are sent via Brevo's HTTPS API instead of SMTP.
    // This matters because Render's free tier blocks outbound SMTP ports (25/465/587),
    // so Gmail SMTP works locally but silently fails once deployed there. Brevo's API
    // goes over plain HTTPS (port 443), which is never blocked.
    @Value("${app.brevo.api-key:}")
    private String brevoApiKey;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a welcome email with login credentials to a newly created maintenance officer.
     * Uses Brevo's HTTP API if BREVO_API_KEY is configured (works anywhere, including
     * Render's free tier); otherwise falls back to Gmail SMTP (fine for local development).
     * Returns true if the email was sent successfully, false otherwise (caller should have a fallback).
     */
    public boolean sendOfficerWelcomeEmail(String toEmail, String fullName, String tempPassword) {
        String subject = "Your Maintenance Officer Account - University Maintenance System";
        String body =
                "Hello " + fullName + ",\n\n" +
                        "An administrator has created a Maintenance Officer account for you on the " +
                        "University Maintenance Request System.\n\n" +
                        "Login email: " + toEmail + "\n" +
                        "Temporary password: " + tempPassword + "\n\n" +
                        "Please log in here: " + frontendUrl + "/login\n\n" +
                        "You will be asked to set your own password the first time you log in.\n\n" +
                        "Regards,\n" +
                        "University Maintenance System";

        if (StringUtils.hasText(brevoApiKey)) {
            return sendViaBrevoApi(toEmail, fullName, subject, body);
        }
        return sendViaSmtp(toEmail, subject, body);
    }

    private boolean sendViaBrevoApi(String toEmail, String toName, String subject, String textBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("Accept", "application/json");

            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", "University Maintenance System", "email", fromAddress),
                    "to", List.of(Map.of("email", toEmail, "name", toName)),
                    "subject", subject,
                    "textContent", textBody
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send officer welcome email via Brevo: " + e.getMessage());
            return false;
        }
    }

    private boolean sendViaSmtp(String toEmail, String subject, String textBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(textBody);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // Don't let a failed/unconfigured mail server block officer creation.
            System.err.println("Failed to send officer welcome email via SMTP: " + e.getMessage());
            return false;
        }
    }
}
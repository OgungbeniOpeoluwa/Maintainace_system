package com.miva.maintenance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a welcome email with login credentials to a newly created maintenance officer.
     * Returns true if the email was sent successfully, false otherwise (caller should have a fallback).
     */
    public boolean sendOfficerWelcomeEmail(String toEmail, String fullName, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your Maintenance Officer Account - University Maintenance System");
            message.setText(
                    "Hello " + fullName + ",\n\n" +
                    "An administrator has created a Maintenance Officer account for you on the " +
                    "University Maintenance Request System.\n\n" +
                    "Login email: " + toEmail + "\n" +
                    "Temporary password: " + tempPassword + "\n\n" +
                    "Please log in here: " + frontendUrl + "/login\n\n" +
                    "You will be asked to set your own password the first time you log in.\n\n" +
                    "Regards,\n" +
                    "University Maintenance System"
            );
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // Don't let a failed/unconfigured mail server block officer creation.
            System.err.println("Failed to send officer welcome email: " + e.getMessage());
            return false;
        }
    }
}

package com.univ.maturity.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public boolean sendVerificationEmail(String toEmail, String code) {
        String subject = "Verification Code";
        String message = "Your verification code is: " + code + "\n\nThis code is valid for 10 minutes.";

        if (!mailEnabled) {
            logger.info("Email disabled. Verification code generated for {}", maskEmail(toEmail));
            return false;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Email is enabled but spring.mail.username is empty.");
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(fromEmail);
        email.setTo(toEmail);
        email.setSubject(subject);
        email.setText(message);

        try {
            mailSender.send(email);
            return true;
        } catch (MailException e) {
            logger.error("Failed to send verification email to {}", maskEmail(toEmail), e);
            throw e;
        }
    }

    public boolean sendInvitationEmail(String toEmail, String teamName, String invitationLink) {
        String subject = "Invitation to join team " + teamName;
        String message = "You have been invited to join the team " + teamName + ".\n\n" +
                         "Please click the link below to register and join automatically:\n" +
                         invitationLink;

        if (!mailEnabled) {
            logger.info("Email disabled. Invitation generated for {}", maskEmail(toEmail));
            return false;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Email is enabled but spring.mail.username is empty.");
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(fromEmail);
        email.setTo(toEmail);
        email.setSubject(subject);
        email.setText(message);

        try {
            mailSender.send(email);
            return true;
        } catch (MailException e) {
            logger.error("Failed to send invitation email to {}", maskEmail(toEmail), e);
            throw e;
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "<empty>";
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        String domain = email.substring(at);
        return email.charAt(0) + "***" + domain;
    }
}

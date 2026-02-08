package com.univ.maturity.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String code) {
        String subject = "Verification Code";
        String message = "Your verification code is: " + code + "\n\nThis code is valid for 10 minutes.";
        
        System.out.println("==========================================");
        System.out.println("VERIFICATION CODE for " + toEmail + ": " + code);
        System.out.println("==========================================");

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromEmail);
            email.setTo(toEmail);
            email.setSubject(subject);
            email.setText(message);

            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Failed to send verification email to " + toEmail + ": " + e.getMessage());
        }
    }

    public void sendInvitationEmail(String toEmail, String teamName, String invitationLink) {
        String subject = "Invitation to join team " + teamName;
        String message = "You have been invited to join the team " + teamName + ".\n\n" +
                         "Please click the link below to register and join automatically:\n" +
                         invitationLink;
        
        System.out.println("==========================================");
        System.out.println("INVITATION LINK for " + toEmail + ": " + invitationLink);
        System.out.println("==========================================");

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromEmail);
            email.setTo(toEmail);
            email.setSubject(subject);
            email.setText(message);

            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}

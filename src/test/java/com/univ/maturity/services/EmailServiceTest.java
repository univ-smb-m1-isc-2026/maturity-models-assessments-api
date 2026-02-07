package com.univ.maturity.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    public void sendVerificationEmail_ShouldSendEmail() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");

        String toEmail = "user@example.com";
        String code = "123456";

        emailService.sendVerificationEmail(toEmail, code);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}

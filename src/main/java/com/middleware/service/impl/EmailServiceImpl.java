package com.middleware.service.impl;

import com.middleware.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendOtpEmail(String to, String otp, String name) {
        sendEmail(to,
                "KOINS - Password Reset OTP",
                String.format("Hello %s,\n\nYour OTP for password reset is: %s\n\n" +
                        "This OTP expires in 5 minutes.\n\n" +
                        "If you did not request this, please ignore this email.\n\nKOINS Team", name, otp));
    }

    @Async
    @Override
    public void sendLoanApprovalEmail(String to, String name, String loanAmount) {
        sendEmail(to,
                "KOINS - Loan Approved \uD83C\uDF89",
                String.format("Hello %s,\n\nGreat news! Your loan application of NGN %s has been approved.\n\n" +
                        "The funds will be disbursed to your wallet shortly.\n\nKOINS Team", name, loanAmount));
    }

    @Async
    @Override
    public void sendRepaymentReminderEmail(String to, String name, String amount, String dueDate) {
        sendEmail(to,
                "KOINS - Loan Repayment Reminder",
                String.format("Hello %s,\n\nThis is a reminder that your loan repayment of NGN %s is due on %s.\n\n" +
                                "Please ensure sufficient funds are available in your wallet.\n\nKOINS Team",
                        name, amount, dueDate));
    }

    @Async
    @Override
    public void sendRepaymentSuccessEmail(String to, String name, String amount) {
        sendEmail(to,
                "KOINS - Repayment Successful \u2705",
                String.format("Hello %s,\n\nYour loan repayment of NGN %s has been processed successfully.\n\n" +
                        "Thank you for keeping up with your repayments!\n\nKOINS Team", name, amount));
    }

    @Async
    @Override
    public void sendWalletFundingEmail(String to, String name, String amount, String reference) {
        sendEmail(to,
                "KOINS - Wallet Funded Successfully \uD83D\uDCB0",
                String.format("Hello %s,\n\nYour wallet has been funded with NGN %s.\n\n" +
                                "Transaction Reference: %s\n\n" +
                                "Your new balance is now updated. Thank you for using KOINS!\n\nKOINS Team",
                        name, amount, reference));
    }

    @Override
    public void sendVerificationOtpEmail(String to, String fullName, String otp) {
        String subject = "Verify Your Email Address";

        String body = String.format("""
                    Hello %s,
                    
                    Welcome to KOINS.

                    Your email verification OTP is:

                    %s

                    This OTP will expire in 5 minutes.

                    If you did not create an account, please ignore this email.

                    Regards,
                    KOINS Team
                    """, fullName, otp
        );

        sendEmail(to, subject, body);
    }

    @Override
    public void sendPasswordResetOtpEmail(String to, String fullName, String otp) {

        String subject = "Password Reset OTP";

        String body = String.format("""
                    Hello %s,

                    We received a request to reset your password.

                    Your password reset OTP is:

                    %s

                    This OTP will expire in 5 minutes.

                    If you did not request a password reset, please ignore this email and secure your account.

                    Regards,
                    KOINS Team
                    """,
                        fullName,
                        otp
                );

        sendEmail(
                to,
                subject,
                body
        );
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

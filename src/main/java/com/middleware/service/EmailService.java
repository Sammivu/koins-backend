package com.middleware.service;

public interface EmailService {
    void sendOtpEmail(String to, String otp, String name);
    void sendLoanApprovalEmail(String to, String name, String loanAmount);
    void sendRepaymentReminderEmail(String to, String name, String amount, String dueDate);
    void sendRepaymentSuccessEmail(String to, String name, String amount);
    void sendWalletFundingEmail(String to, String name, String amount, String reference);

    void sendVerificationOtpEmail(String to, String fullName, String otp);

    void sendPasswordResetOtpEmail(String to, String fullName, String otp);
}

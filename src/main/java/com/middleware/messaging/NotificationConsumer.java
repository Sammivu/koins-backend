package com.middleware.messaging;

import com.middleware.config.RabbitMQConfig;
import com.middleware.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment confirmation events.
 * Handles post-payment actions: email, audit logging, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CONFIRMATION_QUEUE)
    public void handlePaymentConfirmation(PaymentConfirmationMessage message) {
        log.info("Consuming payment confirmation: event={}, ref={}",
                message.getPaymentEvent(), message.getReferenceNumber());

        try {
            switch (message.getPaymentEvent()) {
                case WALLET_FUNDED -> {
                    log.info("Wallet funded for user={}, amount={} {}",
                            message.getUserEmail(), message.getAmount(), message.getCurrency());
                    emailService.sendWalletFundingEmail(
                            message.getUserEmail(),
                            message.getUserFullName(),
                            message.getAmount().toPlainString(),
                            message.getReferenceNumber()
                    );
                }
                case PAYMENT_FAILED -> log.warn("Payment failed for user={}, ref={}",
                        message.getUserEmail(), message.getReferenceNumber());
            }
        } catch (Exception e) {
            log.error("Error processing payment confirmation for ref={}: {}",
                    message.getReferenceNumber(), e.getMessage());
            throw e; // Re-throw so RabbitMQ routes to DLQ after retry exhaustion
        }
    }

    @RabbitListener(queues = RabbitMQConfig.LOAN_NOTIFICATION_QUEUE)
    public void handleLoanNotification(LoanNotificationMessage message) {
        log.info("Consuming loan notification: event={}, loanId={}",
                message.getEventType(), message.getLoanId());

        try {
            switch (message.getEventType()) {
                case LOAN_APPROVED -> emailService.sendLoanApprovalEmail(
                        message.getUserEmail(),
                        message.getUserFullName(),
                        message.getLoanAmount().toPlainString()
                );
                case LOAN_DISBURSED -> log.info("Loan {} disbursed to user {}. Email handled separately.",
                        message.getLoanId(), message.getUserEmail());
                case LOAN_REPAID -> emailService.sendRepaymentSuccessEmail(
                        message.getUserEmail(),
                        message.getUserFullName(),
                        message.getLoanAmount().toPlainString()
                );
                case LOAN_DEFAULTED -> log.warn("Loan {} is DEFAULTED for user {}",
                        message.getLoanId(), message.getUserEmail());
                default -> log.debug("No email action for event type: {}", message.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing loan notification for loanId={}: {}",
                    message.getLoanId(), e.getMessage());
            throw e; // Re-throw so RabbitMQ routes to DLQ after retry exhaustion
        }
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_VERIFICATION_QUEUE)
    public void handleEmailVerificationOtp(EmailVerificationMessage message) {
        log.info("Consuming email verification OTP for {}", message.getUserEmail());
        try {
            emailService.sendVerificationOtpEmail(
                    message.getUserEmail(),
                    message.getFullName(),
                    message.getOtp()
            );
        } catch (Exception e) {
            log.error("Failed email verification OTP for {}: {}", message.getUserEmail(), e.getMessage());
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PASSWORD_RESET_QUEUE)
    public void handlePasswordResetOtp(EmailVerificationMessage message) {

        log.info("Consuming password reset OTP for {}", message.getUserEmail());
        try {
            emailService.sendPasswordResetOtpEmail(
                    message.getUserEmail(),
                    message.getFullName(),
                    message.getOtp()
            );
        } catch (Exception e) {
            log.error("Failed password reset OTP for {}: {}", message.getUserEmail(), e.getMessage());
            throw e;
        }
    }
}

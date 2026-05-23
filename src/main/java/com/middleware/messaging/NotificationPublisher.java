package com.middleware.messaging;


import com.middleware.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a loan lifecycle event to the loan notifications queue.
     */
    public void publishLoanNotification(LoanNotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.KOINS_EXCHANGE,
                    RabbitMQConfig.LOAN_NOTIFICATION_ROUTING_KEY,
                    message
            );
            log.info("Published loan notification: event={}, loanId={}, user={}",
                    message.getEventType(), message.getLoanId(), message.getUserEmail());
        } catch (AmqpException e) {
            log.error("Failed to publish loan notification for loanId={}: {}",
                    message.getLoanId(), e.getMessage());
        }
    }

    /**
     * Publishes a payment confirmation event after Paystack webhook verification.
     */
    public void publishPaymentConfirmation(PaymentConfirmationMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.KOINS_EXCHANGE,
                    RabbitMQConfig.PAYMENT_CONFIRMATION_ROUTING_KEY,
                    message
            );
            log.info("Published payment confirmation: event={}, ref={}, user={}",
                    message.getPaymentEvent(), message.getReferenceNumber(), message.getUserEmail());
        } catch (AmqpException e) {
            log.error("Failed to publish payment confirmation for ref={}: {}",
                    message.getReferenceNumber(), e.getMessage());
        }
    }

    /**
     * Publishes email
     */
    public void publishEmailVerificationOtp(EmailVerificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.KOINS_EXCHANGE,
                    RabbitMQConfig.EMAIL_VERIFICATION_ROUTING_KEY,
                    message
            );
            log.info("Published email verification OTP for {}", message.getUserEmail());

        } catch (AmqpException e) {
            log.error("Failed to publish email verification OTP for {}: {}", message.getUserEmail(),
                    e.getMessage()
            );
        }
    }
    public void publishPasswordResetOtp(EmailVerificationMessage message) {

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.KOINS_EXCHANGE,
                    RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY,
                    message
            );
            log.info("Published password reset OTP for {}", message.getUserEmail());
        } catch (AmqpException e) {

            log.error("Failed to publish password reset OTP for {}: {}", message.getUserEmail(), e.getMessage());
        }
    }

}
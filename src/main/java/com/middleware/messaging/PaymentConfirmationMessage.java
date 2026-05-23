package com.middleware.messaging;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message payload published to the payment confirmations queue.
 * Published after a Paystack webhook is verified and wallet is credited.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmationMessage implements Serializable {

    public enum PaymentEvent {
        WALLET_FUNDED,
        PAYMENT_FAILED
    }

    private String transactionId;
    private String referenceNumber;
    private String externalReference;
    private String userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal amount;
    private String currency;
    private PaymentEvent paymentEvent;
    private LocalDateTime occurredAt;
}
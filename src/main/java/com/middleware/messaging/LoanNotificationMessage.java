package com.middleware.messaging;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message payload published to the loan notifications queue.
 * Used for: loan approval, disbursement, repayment success.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanNotificationMessage implements Serializable {

    public enum EventType {
        LOAN_APPLIED,
        LOAN_APPROVED,
        LOAN_DISBURSED,
        LOAN_REPAID,
        LOAN_DEFAULTED
    }

    private String loanId;
    private String userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private String loanStatus;
    private EventType eventType;
    private LocalDateTime occurredAt;
}
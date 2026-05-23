package com.middleware.entity;

import com.middleware.entity.enums.LoanStatus;
import com.middleware.util.BalanceEncryptionConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "loan_id", updatable = false, nullable = false)
    private UUID loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = BalanceEncryptionConverter.class)
    @Column(name = "loan_amount_encrypted", nullable = false)
    private BigDecimal loanAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "loan_duration_days", nullable = false)
    private Integer loanDurationDays;

    @Convert(converter = BalanceEncryptionConverter.class)
    @Column(name = "total_repayment_encrypted")
    private BigDecimal totalRepaymentAmount;

    @Convert(converter = BalanceEncryptionConverter.class)
    @Column(name = "remaining_balance_encrypted")
    private BigDecimal remainingBalance;

    @Convert(converter = BalanceEncryptionConverter.class)
    @Column(name = "amount_repaid_encrypted")
    @Builder.Default
    private BigDecimal amountRepaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loan_status", nullable = false, length = 20)
    @Builder.Default
    private LoanStatus loanStatus = LoanStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "repayment_schedule", columnDefinition = "json")
    private String repaymentSchedule;

    @Column(name = "loan_integrity_hash")
    private String loanIntegrityHash;

    @Column(name = "repayment_due_date")
    private LocalDate repaymentDueDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
}

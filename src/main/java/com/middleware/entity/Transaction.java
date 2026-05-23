package com.middleware.entity;

import com.middleware.entity.enums.TransactionStatus;
import com.middleware.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "transaction_status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus transactionStatus = TransactionStatus.PENDING;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "description")
    private String description;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

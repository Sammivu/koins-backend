package com.middleware.entity;

import com.middleware.entity.enums.WalletStatus;
import com.middleware.util.BalanceEncryptionConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Convert(converter = BalanceEncryptionConverter.class)
    @Column(name = "wallet_balance_encrypted", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;
//
//    @Transient
//    private BigDecimal walletBalance;

//    @Column(name = "wallet_balance_encrypted")
//    private String walletBalanceEncrypted;

    @Column(name = "wallet_balance_hash")
    private String walletBalanceHash;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "NGN";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "wallet_status", nullable = false, length = 20)
    @Builder.Default
    private WalletStatus walletStatus = WalletStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Version
    private Long version;
}

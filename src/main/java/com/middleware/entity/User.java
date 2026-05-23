package com.middleware.entity;

import com.middleware.entity.enums.AccountStatus;
import com.middleware.entity.enums.OtpAction;
import com.middleware.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "bvn_nin")
    private String bvnNin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "account_status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "otp_action", nullable = false, length = 30)
    @Builder.Default
    private OtpAction otpAction = OtpAction.EMAIL_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "otp_attempt_count", nullable = false)
    @Builder.Default
    private Integer otpAttemptCount = 0;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // OTP fields
    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
}

package com.middleware.security;

import com.middleware.entity.Loan;
import com.middleware.entity.Wallet;
import com.middleware.entity.enums.LoanStatus;
import com.middleware.exception.InternalServerException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;


@Slf4j
@Component
public class CryptoUtil {

    @Value("${app.crypto.secret}")
    private String secret;

    private SecretKeySpec aesKey;

    @PostConstruct
    public void init() {
        aesKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES");
    }

    public String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, aesKey);

            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));

        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Hashing
     * @param walletId
     * @param userId
     * @param balance
     * @return
     */
    public String generateWalletSignature(UUID walletId, UUID userId, BigDecimal balance)  {
        try {
            String normalizedBalance =
                    balance.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            String payload = walletId + "|" + userId + "|" + normalizedBalance;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {

            throw new InternalServerException("Unable to generate wallet signature", e);
        }
    }

    public void validateWalletHash(Wallet wallet) {

        String expectedHash = generateWalletSignature(
                        wallet.getWalletId(),
                        wallet.getUser().getId(),
                        wallet.getWalletBalance()
                );

        if (!expectedHash.equals(wallet.getWalletBalanceHash())) {
            throw new SecurityException("Wallet balance tampering detected");
        }
    }

    public void validateLoanHash(Loan loan) {

        String expectedHash = generateLoanSignature(
                        loan.getLoanId(),
                        loan.getUser().getId(),
                        loan.getLoanAmount(),
                        loan.getTotalRepaymentAmount(),
                        loan.getAmountRepaid(),
                        loan.getRemainingBalance()
        );

        if (!expectedHash.equals(loan.getLoanIntegrityHash())) {
            throw new SecurityException("Loan data tampering detected");
        }
    }

    public String generateLoanSignature(UUID loanId, UUID userId, BigDecimal loanAmount, BigDecimal totalRepaymentAmount,
                                        BigDecimal amountRepaid, BigDecimal remainingBalance) {
        try {
            String normalizedLoanAmount = normalizeBigDecimal(loanAmount);
            String normalizedTotalRepayment = normalizeBigDecimal(totalRepaymentAmount);
            String normalizedAmountRepaid = normalizeBigDecimal(amountRepaid);
            String normalizedRemainingBalance = normalizeBigDecimal(remainingBalance);

            String payload = loanId + "|" + userId + "|" + normalizedLoanAmount + "|" + normalizedTotalRepayment + "|" +
                            normalizedAmountRepaid + "|" + normalizedRemainingBalance;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {

            throw new InternalServerException("Unable to generate loan signature", e);
        }
    }
    private String normalizeBigDecimal(BigDecimal value) {

        if (value == null) {
            return "0";
        }
        return value.setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
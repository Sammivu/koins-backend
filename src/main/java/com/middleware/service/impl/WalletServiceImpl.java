package com.middleware.service.impl;

import com.middleware.dto.request.FundWalletRequest;
import com.middleware.dto.response.*;
import com.middleware.entity.*;
import com.middleware.entity.enums.*;
import com.middleware.exception.BadRequestException;
import com.middleware.exception.ResourceNotFoundException;
import com.middleware.messaging.NotificationPublisher;
import com.middleware.messaging.PaymentConfirmationMessage;
import com.middleware.repository.*;
import com.middleware.security.CryptoUtil;
import com.middleware.service.PaystackService;
import com.middleware.service.WalletService;
import com.middleware.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    @Value("${app.crypto.secret}")
    private String secret;

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PaystackService paystackService;
    private final Util referenceUtil;
    private final NotificationPublisher notificationPublisher;
    private final CryptoUtil cryptoUtil;

    @Override
    public ApiResponse<WalletResponse> getWalletBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        cryptoUtil.validateWalletHash(wallet);
        return ApiResponse.success("Wallet balance retrieved", toWalletResponse(wallet));
    }

    @Transactional
    public void repairWalletHashes() {

        List<Wallet> wallets = walletRepository.findAll();

        for (Wallet wallet : wallets) {

            String hash = cryptoUtil.generateWalletSignature(wallet.getWalletId(),
                    wallet.getUser().getId(),
                    wallet.getWalletBalance()
            );

            wallet.setWalletBalanceHash(hash);
        }

        walletRepository.saveAll(wallets);

        log.info("Wallet hashes repaired successfully");
    }

    @Override
    @Transactional
    public ApiResponse<PaymentInitResponse> initiateFunding(String email, FundWalletRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        String reference = referenceUtil.generate("FUND");

        Transaction pendingTx = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .transactionType(TransactionType.CREDIT)
                .amount(request.getAmount())
                .transactionStatus(TransactionStatus.PENDING)
                .referenceNumber(reference)
                .description("Wallet funding via Paystack")
                .build();
        transactionRepository.save(pendingTx);

        PaymentInitResponse paymentData = paystackService.initializePayment(
                email, request.getAmount(), reference, request.getCallbackUrl());

        log.info("Payment initialized for user: {}, ref: {}", email, reference);
        return ApiResponse.success("Payment initialized. Complete payment via the authorization URL.", paymentData);
    }

    @Override
    public ApiResponse<Page<TransactionResponse>> getTransactionHistory(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Page<TransactionResponse> transactions = transactionRepository
                .findByWallet(wallet, pageable)
                .map(this::toTransactionResponse);
        return ApiResponse.success("Transaction history retrieved", transactions);
    }

    // ── Called internally after webhook confirms payment ────────────
    @Override
    @Transactional
    public void creditWalletAfterPayment(Transaction transaction, String externalReference) {
        Wallet wallet = transaction.getWallet();
        cryptoUtil.validateWalletHash(wallet);

        BigDecimal updatedBalance = wallet.getWalletBalance().add(transaction.getAmount());
        wallet.setWalletBalance(updatedBalance);
        wallet.setWalletBalanceHash(
                cryptoUtil.generateWalletSignature(wallet.getWalletId(), wallet.getUser().getId(), updatedBalance));
        walletRepository.save(wallet);

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setExternalReference(externalReference);
        transactionRepository.save(transaction);

        // Publish payment confirmation event
        notificationPublisher.publishPaymentConfirmation(PaymentConfirmationMessage.builder()
                        .transactionId(transaction.getTransactionId().toString())
                        .referenceNumber(transaction.getReferenceNumber())
                        .externalReference(externalReference)
                        .userId(transaction.getUser().getId().toString())
                        .userEmail(transaction.getUser().getEmail())
                        .userFullName(transaction.getUser().getFullName())
                        .amount(transaction.getAmount())
                        .currency(wallet.getCurrency())
                        .paymentEvent(PaymentConfirmationMessage.PaymentEvent.WALLET_FUNDED)
                        .occurredAt(LocalDateTime.now())
                        .build()
        );
    }

    @Override
    @Transactional
    public ApiResponse<PaymentVerificationResponse> verifyPayment(String email, String reference) {
        log.info("Manually verifying payment");
        User user = userRepository.findByEmail(email).orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Transaction transaction = transactionRepository
                .findByReferenceNumber(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Security check
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot verify another user's payment");
        }

        // Idempotency
        if (transaction.getTransactionStatus() == TransactionStatus.SUCCESS) {

            return ApiResponse.success("Payment already processed",
                    PaymentVerificationResponse.builder()
                            .reference(transaction.getReferenceNumber())
                            .externalReference(transaction.getExternalReference())
                            .paymentStatus("success")
                            .amount(transaction.getAmount())
                            .currency(transaction.getWallet().getCurrency())
                            .walletCredited(true)
                            .message("Wallet already credited")
                            .build()
            );
        }
        PaymentVerificationResponse paystackResponse = paystackService.verifyTransaction(reference);
        String status = paystackResponse.getPaymentStatus();
        switch (status.toLowerCase()) {
            case "success" -> {
                creditWalletAfterPayment(transaction, paystackResponse.getExternalReference());
                paystackResponse.setWalletCredited(true);
                paystackResponse.setMessage(
                        "Wallet credited successfully"
                );

                return ApiResponse.success("Payment verified successfully", paystackResponse);
            }

            case "failed", "abandoned", "reversed" -> {
                transaction.setTransactionStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                paystackResponse.setWalletCredited(false);
                paystackResponse.setMessage("Payment was not successful");
                return ApiResponse.success("Payment verification completed", paystackResponse);
            }
            default -> throw new BadRequestException("Payment is still pending");
        }
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .walletBalance(wallet.getWalletBalance())
                .currency(wallet.getCurrency())
                .walletStatus(wallet.getWalletStatus().name())
                .createdDate(wallet.getCreatedDate())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .transactionType(t.getTransactionType().name())
                .amount(t.getAmount())
                .transactionStatus(t.getTransactionStatus().name())
                .referenceNumber(t.getReferenceNumber())
                .externalReference(t.getExternalReference())
                .description(t.getDescription())
                .timestamp(t.getTimestamp())
                .build();
    }
}

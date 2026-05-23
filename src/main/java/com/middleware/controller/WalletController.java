package com.middleware.controller;

import com.middleware.config.Audit;
import com.middleware.dto.request.FundWalletRequest;
import com.middleware.dto.request.PaymentVerificationRequest;
import com.middleware.dto.response.*;
import com.middleware.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wallet", description = "Wallet management APIs")
public class WalletController {

    private final WalletService walletService;

    @Audit(action = "INITIATE_PAYMENT", entityType = "USER")
    @GetMapping("/balance")
    @Operation(summary = "Get wallet balance")
    public ResponseEntity<ApiResponse<WalletResponse>> getBalance(Authentication authentication) {
        return ResponseEntity.ok(walletService.getWalletBalance(authentication.getName()));
    }

    @Audit(action = "INITIATE_PAYMENT", entityType = "TRANSACTION")
    @PostMapping("/fund")
    @Operation(summary = "Initiate wallet funding via Paystack")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> fundWallet(@Valid @RequestBody FundWalletRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(walletService.initiateFunding(authentication.getName(), request));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get wallet transaction history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(Authentication authentication,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {
        return ResponseEntity.ok(walletService.getTransactionHistory(authentication.getName(), pageable));
    }

    @Audit(action = "VERIFY_PAYMENT", entityType = "TRANSACTION")
    @PostMapping("/verify-payment")
    @Operation(summary = "Manually verify Paystack payment")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request,
                                                                                  Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(walletService.verifyPayment(email, request.getReference()));
    }
}

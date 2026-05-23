package com.middleware.service;

import com.middleware.dto.request.*;
import com.middleware.dto.response.*;
import com.middleware.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface WalletService {
    ApiResponse<WalletResponse> getWalletBalance(String email);
    ApiResponse<PaymentInitResponse> initiateFunding(String email, FundWalletRequest request);
    ApiResponse<Page<TransactionResponse>> getTransactionHistory(String email, Pageable pageable);

    // ── Called internally after webhook confirms payment ────────────
    @Transactional
    void creditWalletAfterPayment(Transaction transaction, String externalReference);

    @Transactional
    ApiResponse<PaymentVerificationResponse> verifyPayment(String email, String reference);
}

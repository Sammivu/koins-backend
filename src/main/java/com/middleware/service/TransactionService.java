package com.middleware.service;

import com.middleware.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface TransactionService {
    ApiResponse<Page<TransactionResponse>> getAllTransactions(String email, Pageable pageable);
    ApiResponse<TransactionResponse> getTransactionById(UUID transactionId);
}

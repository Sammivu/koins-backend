package com.middleware.service.impl;


import com.middleware.dto.response.ApiResponse;
import com.middleware.dto.response.TransactionResponse;
import com.middleware.entity.Transaction;
import com.middleware.entity.User;
import com.middleware.exception.ResourceNotFoundException;
import com.middleware.repository.TransactionRepository;
import com.middleware.repository.UserRepository;
import com.middleware.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    public ApiResponse<Page<TransactionResponse>> getAllTransactions(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Page<TransactionResponse> transactions = transactionRepository
                .findByUser(user, pageable).map(this::toResponse);
        return ApiResponse.success("Transactions retrieved", transactions);
    }

    @Override
    public ApiResponse<TransactionResponse> getTransactionById(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return ApiResponse.success("Transaction retrieved", toResponse(transaction));
    }

    private TransactionResponse toResponse(Transaction t) {
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

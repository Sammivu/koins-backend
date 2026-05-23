package com.middleware.controller;

import com.middleware.config.Audit;
import com.middleware.dto.response.ApiResponse;
import com.middleware.dto.response.TransactionResponse;
import com.middleware.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Transaction history APIs")
public class TransactionController {

    private final TransactionService transactionService;

    @Audit(action = "ALL_USER_TRANSACTION", entityType = "TRANSACTION")
    @GetMapping
    @Operation(summary = "List all transactions for authenticated user")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(Authentication authentication,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(authentication.getName(), pageable));
    }

    @Audit(action = "USER_TRANSACTION", entityType = "TRANSACTION")
    @GetMapping("/{transactionId}")
    @Operation(summary = "Get a single transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }
}

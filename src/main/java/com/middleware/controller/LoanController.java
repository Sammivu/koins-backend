package com.middleware.controller;

import com.middleware.config.Audit;
import com.middleware.dto.request.LoanApplicationRequest;
import com.middleware.dto.request.LoanRepaymentRequest;
import com.middleware.dto.response.ApiResponse;
import com.middleware.dto.response.LoanResponse;
import com.middleware.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Loans", description = "Loan management APIs")
public class LoanController {

    private final LoanService loanService;

    @Audit(action = "REQUEST_LOAN", entityType = "USER")
    @PostMapping("/apply")
    @Operation(summary = "Apply for a loan")
    public ResponseEntity<ApiResponse<LoanResponse>> applyForLoan(@Valid @RequestBody LoanApplicationRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.applyForLoan(authentication.getName(), request));
    }

    @Audit(action = "LOAN_APPROVAL", entityType = "ADMIN")
    @PutMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a loan (Admin only)")
    public ResponseEntity<ApiResponse<LoanResponse>> approveLoan(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanService.approveLoan(loanId));
    }

    @Audit(action = "DISBURSEMENT", entityType = "ADMIN")
    @PutMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disburse an approved loan (Admin only)")
    public ResponseEntity<ApiResponse<LoanResponse>> disburseLoan(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanService.disburseLoan(loanId));
    }

    @Audit(action = "LOAN_REPAYMENT", entityType = "USER")
    @PostMapping("/{loanId}/repay")
    @Operation(summary = "Repay a loan")
    public ResponseEntity<ApiResponse<LoanResponse>> repayLoan(@PathVariable UUID loanId, @Valid @RequestBody LoanRepaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(loanService.repayLoan(authentication.getName(), loanId, request));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan details")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanService.getLoanById(loanId));
    }

    @GetMapping
    @Operation(summary = "List all loans for authenticated user")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getMyLoans(Authentication authentication) {
        return ResponseEntity.ok(loanService.getUserLoans(authentication.getName()));
    }

    @Audit(action = "LOAN_RECORDS", entityType = "ADMIN")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all loans in the system (Admin only)")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }
}

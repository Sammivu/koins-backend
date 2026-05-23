package com.middleware.service;

import com.middleware.dto.request.*;
import com.middleware.dto.response.*;
import java.util.List;
import java.util.UUID;

public interface LoanService {
    ApiResponse<LoanResponse> applyForLoan(String email, LoanApplicationRequest request);
    ApiResponse<LoanResponse> approveLoan(UUID loanId);
    ApiResponse<LoanResponse> disburseLoan(UUID loanId);
    ApiResponse<LoanResponse> repayLoan(String email, UUID loanId, LoanRepaymentRequest request);
    ApiResponse<LoanResponse> getLoanById(UUID loanId);
    ApiResponse<List<LoanResponse>> getUserLoans(String email);
    ApiResponse<List<LoanResponse>> getAllLoans();
}

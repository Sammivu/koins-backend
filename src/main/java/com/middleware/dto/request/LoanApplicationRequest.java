package com.middleware.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1000 NGN")
    private BigDecimal loanAmount;

    @NotNull
    @DecimalMin(value = "1") @DecimalMax(value = "100")
    private BigDecimal interestRate;

    @NotNull
    @Min(value = 1, message = "Loan duration must be at least 1 day")
    @Max(value = 365, message = "Loan duration cannot exceed 365 days")
    private Integer loanDurationDays;
}

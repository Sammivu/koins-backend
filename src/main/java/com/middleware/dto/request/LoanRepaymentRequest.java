package com.middleware.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanRepaymentRequest {
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}

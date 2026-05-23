package com.middleware.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FundWalletRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum funding amount is 100 NGN")
    private BigDecimal amount;

    private String callbackUrl;
}

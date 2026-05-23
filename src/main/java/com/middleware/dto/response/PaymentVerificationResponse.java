package com.middleware.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationResponse {

    private String reference;
    private String externalReference;
    private String paymentStatus;
    private BigDecimal amount;
    private String currency;
    private String gatewayResponse;
    private String paidAt;
    private String customerEmail;
    private boolean walletCredited;
    private String message;
}
package com.middleware.service;

import com.middleware.dto.response.PaymentVerificationResponse;
import com.middleware.dto.response.PaymentInitResponse;
import java.math.BigDecimal;

public interface PaystackService {
    PaymentInitResponse initializePayment(String email, BigDecimal amount, String reference, String callbackUrl);
    boolean verifyWebhookSignature(String payload, String paystackSignature);

    PaymentVerificationResponse verifyTransaction(String reference);
}

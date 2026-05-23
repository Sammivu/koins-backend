package com.middleware.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.dto.response.PaymentVerificationResponse;
import com.middleware.dto.response.PaymentInitResponse;
import com.middleware.exception.BadRequestException;
import com.middleware.service.PaystackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackServiceImpl implements PaystackService {

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.base-url:https://api.paystack.co}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentInitResponse initializePayment(String email, BigDecimal amount, String reference, String callbackUrl) {
        // Paystack expects amount in kobo (NGN * 100)
        long amountInKobo = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("amount", amountInKobo);
        payload.put("reference", reference);
        payload.put("currency", "NGN");
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            payload.put("callback_url", callbackUrl);
        }

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(baseUrl + "/transaction/initialize")
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");

            return PaymentInitResponse.builder()
                    .authorizationUrl(data.get("authorization_url").asText())
                    .accessCode(data.get("access_code").asText())
                    .reference(data.get("reference").asText())
                    .build();

        } catch (Exception e) {
            log.error("Paystack initialization error: {}", e.getMessage());
            throw new RuntimeException("Payment initialization failed: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String paystackSignature) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(keySpec);
            byte[] computedHash = sha512Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(computedHash);
            return computedSignature.equals(paystackSignature);
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public PaymentVerificationResponse verifyTransaction(String reference) {

        String url = baseUrl + "/transaction/verify/" + reference;

        try {

            JsonNode response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.get("status").asBoolean()) {
                throw new BadRequestException("Unable to verify transaction");
            }

            JsonNode data = response.get("data");

            BigDecimal amount = BigDecimal.valueOf(data.get("amount").asLong()).divide(BigDecimal.valueOf(100));

            return PaymentVerificationResponse.builder()
                    .reference(data.get("reference").asText())
                    .externalReference(data.get("id").asText())
                    .paymentStatus(data.get("status").asText())
                    .amount(amount)
                    .currency(data.get("currency").asText())
                    .gatewayResponse(data.get("gateway_response").asText())
                    .paidAt(data.path("paid_at").asText(null))
                    .customerEmail(data.path("customer")
                            .path("email")
                            .asText(null)
                    ).build();

        } catch (Exception ex) {
            log.error("Paystack verification failed: {}", ex.getMessage());
            throw new BadRequestException("Unable to verify payment");
        }
    }
}

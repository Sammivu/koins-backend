package com.middleware.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.entity.Transaction;
import com.middleware.entity.Wallet;
import com.middleware.entity.enums.TransactionStatus;
import com.middleware.repository.TransactionRepository;
import com.middleware.repository.WalletRepository;
import com.middleware.service.PaystackService;
import com.middleware.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Payment gateway webhook endpoints")
public class WebhookController {

    private final PaystackService paystackService;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    @PostMapping("/paystack")
    @Operation(summary = "Receive Paystack payment webhook")
    public ResponseEntity<String> handlePaystackWebhook(@RequestBody String payload, @RequestHeader("x-paystack-signature") String signature) {

        log.info("Paystack webhook received");

        if (!paystackService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("event").asText();

            if ("charge.success".equals(eventType)) {
                JsonNode data = event.get("data");
                String reference = data.get("reference").asText();
                String externalRef = data.get("id").asText(); // Paystack transaction ID
                long amountInKobo = data.get("amount").asLong();
                BigDecimal amount = BigDecimal.valueOf(amountInKobo).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                String status = data.get("status").asText();

                Optional<Transaction> txOpt = transactionRepository.findByReferenceNumber(reference);

                if (txOpt.isPresent()) {
                    Transaction transaction = txOpt.get();

                    if (transaction.getTransactionStatus() == TransactionStatus.PENDING && "success".equalsIgnoreCase(status)) {

                        // Delegate to WalletService — it credits wallet AND publishes RabbitMQ event
                        walletService.creditWalletAfterPayment(transaction, externalRef);
                        log.info("Webhook processed: wallet credited. Ref: {}, Amount: {}", reference, amount);
                    } else {
                        log.info("Skipping webhook — tx not PENDING or payment not successful. Ref: {}", reference);
                    }
                } else {
                    log.warn("No transaction found for reference: {}", reference);
                }
            }

        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Processing error");
        }

        return ResponseEntity.ok("OK");
    }
}


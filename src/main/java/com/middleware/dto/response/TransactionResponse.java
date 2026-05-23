package com.middleware.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class TransactionResponse {
    private UUID transactionId;
    private String transactionType;
    private BigDecimal amount;
    private String transactionStatus;
    private String referenceNumber;
    private String externalReference;
    private String description;
    private LocalDateTime timestamp;
}
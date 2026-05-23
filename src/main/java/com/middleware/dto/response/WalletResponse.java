package com.middleware.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class WalletResponse {
    private UUID walletId;
    private BigDecimal walletBalance;
    private String currency;
    private String walletStatus;
    private LocalDateTime createdDate;
}


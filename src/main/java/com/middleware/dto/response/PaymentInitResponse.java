package com.middleware.dto.response;

import lombok.*;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class PaymentInitResponse {
    private String authorizationUrl;
    private String accessCode;
    private String reference;
}

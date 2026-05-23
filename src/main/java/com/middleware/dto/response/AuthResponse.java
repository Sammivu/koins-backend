package com.middleware.dto.response;

import lombok.*;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private String email;
    private String fullName;
//    private String role;
}

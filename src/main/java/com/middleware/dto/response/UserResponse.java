package com.middleware.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String accountStatus;
//    private String role;
    private LocalDateTime createdDate;
}

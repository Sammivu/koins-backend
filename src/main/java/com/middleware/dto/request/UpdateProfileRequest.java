package com.middleware.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phoneNumber;
}

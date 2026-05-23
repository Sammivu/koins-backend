package com.middleware.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    @Size(min = 8)
    private String newPassword;
}

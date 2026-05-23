package com.middleware.dto.request;

import com.middleware.entity.enums.OtpAction;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResendOtpRequest {
    @NotBlank @Email
    private String email;

    private OtpAction otpAction;
}

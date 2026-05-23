package com.middleware.messaging;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userEmail;

    private String fullName;

    private String otp;
}
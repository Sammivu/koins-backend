package com.middleware.service;

import com.middleware.dto.request.*;
import com.middleware.dto.response.*;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    ApiResponse<AuthResponse> register(RegisterRequest request);


    ApiResponse<String> verifyEmailOtp(VerifyEmail request);

    ApiResponse<AuthResponse> login(LoginRequest request);
    ApiResponse<Void> logout(String token);
    ApiResponse<Void> forgotPassword(ForgotPasswordRequest request);
    ApiResponse<Void> verifyOtpAndResetPassword(VerifyOtpRequest request);
    ApiResponse<Void> resendOtp(ResendOtpRequest request);
    ApiResponse<UserResponse> updateProfile(String email, UpdateProfileRequest request);
}

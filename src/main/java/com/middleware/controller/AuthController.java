package com.middleware.controller;

import com.middleware.config.Audit;
import com.middleware.dto.request.*;
import com.middleware.dto.response.*;
import com.middleware.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and account management APIs")
public class AuthController {

    private final AuthService authService;

    @Audit(
            action = "REGISTER_USER",
            entityType = "USER"
    )
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Audit(action = "VERIFY_EMAIL", entityType = "USER")
    @PostMapping("/verify/email")
    @Operation(summary = "Verify email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyEmail request) {
        return ResponseEntity.ok(authService.verifyEmailOtp(request));
    }

    @Audit(action = "LOGIN", entityType = "USER")
    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Audit(action = "LOG_OUT", entityType = "USER")
    @PostMapping("/logout")
    @Operation(summary = "Logout - blacklists the current token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(authService.logout(token));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request OTP for password reset")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and reset password")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtpAndResetPassword(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @Audit(action = "PROFILE_UPDATE", entityType = "USER")
    @PutMapping("/profile")
    @Operation(summary = "Update profile details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }
}

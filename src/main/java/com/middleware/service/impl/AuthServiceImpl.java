package com.middleware.service.impl;

import com.middleware.dto.request.*;
import com.middleware.dto.response.*;
import com.middleware.entity.*;
import com.middleware.entity.enums.*;
import com.middleware.exception.*;
import com.middleware.messaging.EmailVerificationMessage;
import com.middleware.messaging.NotificationPublisher;
import com.middleware.repository.*;
import com.middleware.service.AuthService;
import com.middleware.service.EmailService;
import com.middleware.util.JwtUtil;
import com.middleware.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final NotificationPublisher notificationPublisher;
    private final Util util;

//    @Override
//    @Transactional
//    public ApiResponse<AuthResponse> register(RegisterRequest request) {
//
//
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new BadRequestException("Email already registered");
//        }
//        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
//            throw new BadRequestException("Phone number already registered");
//        }
//        String phone = Util.normalizePhoneNumber(request.getPhoneNumber());
//
//        User user = User.builder()
//                .fullName(request.getFullName())
//                .email(request.getEmail().toLowerCase())
//                .phoneNumber(phone)
//                .password(passwordEncoder.encode(request.getPassword()))
//                .bvnNin(request.getBvnNin())
//                .accountStatus(AccountStatus.ACTIVE)
//                .role(Role.USER)
//                .build();
//        userRepository.save(user);
//
//        // Auto-create wallet on signup
//        Wallet wallet = Wallet.builder()
//                .user(user)
//                .walletBalance(BigDecimal.ZERO)
//                .currency("NGN")
//                .walletStatus(WalletStatus.ACTIVE)
//                .build();
//        walletRepository.save(wallet);
//
//        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
//        log.info("New user registered: {}", user.getEmail());
//        return ApiResponse.success("Registration successful", buildAuthResponse(user, token));
//    }

    @Override
    @Transactional
    public ApiResponse<AuthResponse> register(RegisterRequest request) {
        log.info("Starting new user registration");

        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new ResourceNotFoundException("Email already registered");
        }
        String normalizedPhone = Util.normalizePhoneNumber(request.getPhoneNumber());

        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new ResourceNotFoundException("Phone number already registered");
        }

        String otp = util.generateOtp();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(normalizedPhone)
                .password(passwordEncoder.encode(request.getPassword()))
                .bvnNin(request.getBvnNin())
                .otp(passwordEncoder.encode(otp))
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .otpAttemptCount(0)
                .emailVerified(false)
                .accountStatus(AccountStatus.PENDING)
                .role(Role.USER)
                .build();
        userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(user)
                .walletBalance(BigDecimal.ZERO)
                .currency("NGN")
                .walletStatus(WalletStatus.ACTIVE)
                .build();
        walletRepository.save(wallet);

        notificationPublisher.publishEmailVerificationOtp(EmailVerificationMessage.builder()
                        .userEmail(user.getEmail())
                        .fullName(user.getFullName())
                        .otp(otp)
                        .build());

        return ApiResponse.success("Registration successful. OTP sent to email.", buildAuthResponse(user, null));
    }

    @Override
    @Transactional
    public ApiResponse<String> verifyEmailOtp(VerifyEmail request) {

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email already verified");
        }
        if (user.getOtpAction() != OtpAction.EMAIL_VERIFICATION) {
            throw new BadRequestException("Invalid OTP action");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }

        int attempts = user.getOtpAttemptCount() == null ? 0 : user.getOtpAttemptCount();
        if (attempts >= 5) {
            throw new BadRequestException("Too many failed OTP attempts.");
        }

        if (!passwordEncoder.matches(request.getOtp(), user.getOtp())) {
            user.setOtpAttemptCount(attempts + 1);
            userRepository.save(user);
            throw new BadRequestException("Invalid OTP");
        }

        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttemptCount(0);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        return ApiResponse.success("Email verified successfully");
    }

    @Override
    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new UnauthorizedException("Account is suspended. Contact support.");
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnauthorizedException("Please verify your email first");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("User logged in: {}", user.getEmail());
        return ApiResponse.success("Login successful", buildAuthResponse(user, token));
    }

    @Override
    @Transactional
    public ApiResponse<Void> logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null && jwtUtil.isTokenValid(token)) {
            TokenBlacklist blacklisted = TokenBlacklist.builder()
                    .token(token)
                    .expiresAt(jwtUtil.extractExpiration(token).toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                    .blacklistedAt(LocalDateTime.now())
                    .build();
            tokenBlacklistRepository.save(blacklisted);
        }
        return ApiResponse.success("Logged out successfully");
    }

    @Override
    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        String otp = util.generateOtp();
        user.setOtp(passwordEncoder.encode(otp));
        user.setOtpAction(OtpAction.PASSWORD_RESET);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setOtpAttemptCount(0);
        userRepository.save(user);

        notificationPublisher.publishPasswordResetOtp(EmailVerificationMessage.builder()
                        .userEmail(user.getEmail())
                        .fullName(user.getFullName())
                        .otp(otp)
                        .build()
                );
        log.info("OTP sent to: {}", user.getEmail());
        return ApiResponse.success("OTP sent to your email address");
    }

    @Override
    @Transactional
    public ApiResponse<Void> verifyOtpAndResetPassword(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            throw new BadRequestException("No OTP was requested for this account");
        }
        if (user.getOtpAction() != OtpAction.PASSWORD_RESET) {
            throw new BadRequestException("Invalid OTP action");
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        int attempts = user.getOtpAttemptCount() == null ? 0 : user.getOtpAttemptCount();
        if (attempts >= 5) {
            throw new BadRequestException("Too many failed OTP attempts. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(request.getOtp(), user.getOtp())) {
            user.setOtpAttemptCount(attempts + 1);
            userRepository.save(user);
            throw new BadRequestException("Invalid OTP");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttemptCount(0);
        userRepository.save(user);
        return ApiResponse.success("Password reset successfully");
    }

    @Override
    @Transactional
    public ApiResponse<Void> resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        String otp = util.generateOtp();
        user.setOtp(passwordEncoder.encode(otp));
        user.setOtpAction(request.getOtpAction());
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setOtpAttemptCount(0);
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp, user.getFullName());
        return ApiResponse.success("OTP resent successfully");
    }

    @Override
    @Transactional
    public ApiResponse<UserResponse> updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (StringUtils.hasText(request.getPhoneNumber())) {
            String normalizedPhone = Util.normalizePhoneNumber(request.getPhoneNumber());
            Optional<User> existingUser = userRepository.findByPhoneNumber(normalizedPhone);

            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                throw new ApiException("Phone number already in use", HttpStatus.CONFLICT);
            }
            user.setPhoneNumber(normalizedPhone);
        }
        return ApiResponse.success("Profile updated", toUserResponse(user));
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token).tokenType("Bearer")
                .email(user.getEmail()).fullName(user.getFullName())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId()).fullName(user.getFullName())
                .email(user.getEmail()).phoneNumber(user.getPhoneNumber())
                .accountStatus(user.getAccountStatus().name())
                .createdDate(user.getCreatedDate()).build();
    }
}

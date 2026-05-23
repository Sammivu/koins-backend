package com.middleware.service;

import com.middleware.dto.request.LoginRequest;
import com.middleware.dto.request.RegisterRequest;
import com.middleware.dto.response.ApiResponse;
import com.middleware.dto.response.AuthResponse;
import com.middleware.entity.User;
import com.middleware.entity.enums.AccountStatus;
import com.middleware.entity.enums.Role;
import com.middleware.exception.BadRequestException;
import com.middleware.exception.UnauthorizedException;
import com.middleware.repository.TokenBlacklistRepository;
import com.middleware.repository.UserRepository;
import com.middleware.repository.WalletRepository;
import com.middleware.service.impl.AuthServiceImpl;
import com.middleware.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("John Doe")
                .email("john@example.com")
                .phoneNumber("+2348012345678")
                .password("encodedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.USER)
                .build();
    }

    @Test
    void register_ShouldSucceed_WhenEmailAndPhoneAreUnique() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPhoneNumber("+2348012345678");
        request.setPassword("password123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");

        ApiResponse<AuthResponse> response = authService.register(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Registration successful");
        verify(walletRepository, times(1)).save(any());
    }

    @Test
    void register_ShouldThrow_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPhoneNumber("+2348012345678");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_ShouldSucceed_WithValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");

        ApiResponse<AuthResponse> response = authService.login(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAccessToken()).isEqualTo("mock-jwt-token");
    }

    @Test
    void login_ShouldThrow_WithInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }
}

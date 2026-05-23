package com.middleware.service;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.middleware.dto.request.LoanApplicationRequest;
import com.middleware.dto.response.ApiResponse;
import com.middleware.dto.response.LoanResponse;
import com.middleware.entity.Loan;
import com.middleware.entity.User;
import com.middleware.entity.Wallet;
import com.middleware.entity.enums.AccountStatus;
import com.middleware.entity.enums.LoanStatus;
import com.middleware.entity.enums.Role;
import com.middleware.entity.enums.WalletStatus;
import com.middleware.exception.BadRequestException;
import com.middleware.messaging.NotificationPublisher;
import com.middleware.repository.LoanRepository;
import com.middleware.repository.TransactionRepository;
import com.middleware.repository.UserRepository;
import com.middleware.repository.WalletRepository;
import com.middleware.service.impl.LoanServiceImpl;
import com.middleware.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private Util referenceUtil;

    @InjectMocks
    private LoanServiceImpl loanService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // registers JavaTimeModule for LocalDateTime
    private User testUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() throws Exception {
        var field = LoanServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(loanService, objectMapper);

        testUser = User.builder()
                .id(UUID.randomUUID()).email("john@example.com")
                .fullName("John Doe").role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE).build();

        testWallet = Wallet.builder()
                .walletId(UUID.randomUUID()).user(testUser)
                .walletBalance(new BigDecimal("50000.00"))
                .currency("NGN").walletStatus(WalletStatus.ACTIVE).build();
    }

    @Test
    void applyForLoan_ShouldSucceed_WhenAmountWithinLimit() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setLoanAmount(new BigDecimal("10000.00"));
        request.setInterestRate(new BigDecimal("5.00"));
        request.setLoanDurationDays(30);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(any())).thenReturn(Optional.of(testWallet));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationPublisher).publishLoanNotification(any());

        ApiResponse<LoanResponse> response = loanService.applyForLoan("john@example.com", request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getLoanStatus()).isEqualTo("PENDING");
        assertThat(response.getData().getRepaymentSchedule()).isNotEmpty();
        verify(notificationPublisher).publishLoanNotification(any());
    }

    @Test
    void applyForLoan_ShouldFail_WhenAmountExceedsThreeTimesBalance() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setLoanAmount(new BigDecimal("200000.00")); // wallet is 50k, max is 150k
        request.setInterestRate(new BigDecimal("5.00"));
        request.setLoanDurationDays(30);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(any())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> loanService.applyForLoan("john@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("3x your wallet balance");
    }

    @Test
    void applyForLoan_ShouldFail_WhenWalletIsEmpty() {
        testWallet.setWalletBalance(BigDecimal.ZERO);
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setLoanAmount(new BigDecimal("5000.00"));
        request.setInterestRate(new BigDecimal("5.00"));
        request.setLoanDurationDays(30);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(any())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> loanService.applyForLoan("john@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("funded wallet");
    }

    @Test
    void approveLoan_ShouldFail_WhenLoanIsNotPending() {
        Loan loan = Loan.builder()
                .loanId(UUID.randomUUID()).user(testUser)
                .loanAmount(new BigDecimal("10000.00"))
                .loanStatus(LoanStatus.APPROVED).build();

        when(loanRepository.findById(any())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.approveLoan(loan.getLoanId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING loans can be approved");
    }
}

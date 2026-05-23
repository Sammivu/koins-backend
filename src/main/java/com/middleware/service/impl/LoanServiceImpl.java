package com.middleware.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.config.Audit;
import com.middleware.dto.request.LoanApplicationRequest;
import com.middleware.dto.request.LoanRepaymentRequest;
import com.middleware.dto.response.ApiResponse;
import com.middleware.dto.response.LoanResponse;
import com.middleware.dto.response.LoanResponse.RepaymentInstallment;
import com.middleware.entity.*;
import com.middleware.entity.enums.*;
import com.middleware.exception.*;
import com.middleware.messaging.LoanNotificationMessage;
import com.middleware.messaging.NotificationPublisher;
import com.middleware.repository.*;
import com.middleware.security.CryptoUtil;
import com.middleware.service.LoanService;
import com.middleware.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationPublisher notificationPublisher;
    private final Util referenceUtil;
    private final ObjectMapper objectMapper;
    private final CryptoUtil cryptoUtil;

    @Transactional
    public void repairLoanHashes() {
        List<Loan> loans = loanRepository.findAll();
        for (Loan loan : loans) {

            String hash = cryptoUtil.generateLoanSignature(
                    loan.getLoanId(),
                    loan.getUser().getId(),
                    loan.getLoanAmount(),
                    loan.getTotalRepaymentAmount(),
                    loan.getAmountRepaid(),
                    loan.getRemainingBalance()
            );

            loan.setLoanIntegrityHash(hash);
        }
        loanRepository.saveAll(loans);
        log.info("Loan hashes repaired successfully");
    }

    @Override
    @Transactional
    public ApiResponse<LoanResponse> applyForLoan(String email, LoanApplicationRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getWalletBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("You must have a funded wallet to apply for a loan");
        }

        BigDecimal maxLoanAmount = wallet.getWalletBalance().multiply(BigDecimal.valueOf(3));
        if (request.getLoanAmount().compareTo(maxLoanAmount) > 0) {
            throw new BadRequestException(String.format(
                    "Loan amount cannot exceed 3x your wallet balance (Max: NGN %.2f)", maxLoanAmount));
        }

        List<RepaymentInstallment> schedule = generateRepaymentSchedule(
                request.getLoanAmount(), request.getInterestRate(), request.getLoanDurationDays());

        String scheduleJson;
        try {
            scheduleJson = objectMapper.writeValueAsString(schedule);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize repayment schedule");
        }

        BigDecimal totalRepayable = calculateTotalRepayable(request.getLoanAmount(), request.getInterestRate());
        Loan loan = Loan.builder()
                .user(user)
                .loanAmount(request.getLoanAmount())
                .interestRate(request.getInterestRate())
                .remainingBalance(totalRepayable)
                .totalRepaymentAmount(totalRepayable)
                .loanDurationDays(request.getLoanDurationDays())
                .loanStatus(LoanStatus.PENDING)
                .amountRepaid(BigDecimal.ZERO)
                .repaymentSchedule(scheduleJson)
                .dueDate(LocalDateTime.now().plusDays(request.getLoanDurationDays()))
                .build();
        loanRepository.save(loan);

        String hash = cryptoUtil.generateLoanSignature(loan.getLoanId(), user.getId(), loan.getLoanAmount(), loan.getTotalRepaymentAmount(),
                loan.getAmountRepaid(), loan.getRemainingBalance());
        loan.setLoanIntegrityHash(hash);

        loanRepository.save(loan);
        notificationPublisher.publishLoanNotification(buildLoanMessage(loan, user, LoanNotificationMessage.EventType.LOAN_APPLIED));

        log.info("Loan application submitted. User: {}, Amount: {}", email, request.getLoanAmount());
        return ApiResponse.success("Loan application submitted successfully", toLoanResponse(loan, schedule));
    }

    @Override
    @Transactional
    public ApiResponse<LoanResponse> approveLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getLoanStatus() != LoanStatus.PENDING) {
            throw new BadRequestException("Only PENDING loans can be approved");
        }

        loan.setLoanStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());
        loanRepository.save(loan);

        // Publish to RabbitMQ — consumer sends the approval email
        notificationPublisher.publishLoanNotification(
                buildLoanMessage(loan, loan.getUser(), LoanNotificationMessage.EventType.LOAN_APPROVED));

        log.info("Loan approved: {}", loanId);
        return ApiResponse.success("Loan approved successfully", toLoanResponse(loan, parseSchedule(loan)));
    }

    @Override
    @Transactional
    public ApiResponse<LoanResponse> disburseLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        cryptoUtil.validateLoanHash(loan);
        if (loan.getLoanStatus() != LoanStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED loans can be disbursed");
        }

        Wallet wallet = walletRepository.findByUser(loan.getUser())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        cryptoUtil.validateWalletHash(wallet);
        BigDecimal updatedBalance = wallet.getWalletBalance().add(loan.getLoanAmount());
        updateWalletBalance(wallet, updatedBalance);

        Transaction transaction = Transaction.builder()
                .user(loan.getUser())
                .wallet(wallet)
                .transactionType(TransactionType.LOAN_DISBURSEMENT)
                .amount(loan.getLoanAmount())
                .transactionStatus(TransactionStatus.SUCCESS)
                .referenceNumber(referenceUtil.generate("DISB"))
                .description("Loan disbursement for loan ID: " + loan.getLoanId())
                .build();
        transactionRepository.save(transaction);

        loan.setLoanStatus(LoanStatus.DISBURSED);
        loan.setDisbursedAt(LocalDateTime.now());
        loanRepository.save(loan);

        notificationPublisher.publishLoanNotification(
                buildLoanMessage(loan, loan.getUser(), LoanNotificationMessage.EventType.LOAN_DISBURSED));

        log.info("Loan disbursed: {}, Amount: {}", loanId, loan.getLoanAmount());
        return ApiResponse.success("Loan disbursed successfully", toLoanResponse(loan, parseSchedule(loan)));
    }

    @Override
    @Transactional
    public ApiResponse<LoanResponse> repayLoan(String email, UUID loanId, LoanRepaymentRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        cryptoUtil.validateLoanHash(loan);
        if (!loan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to repay this loan");
        }
        if (loan.getLoanStatus() != LoanStatus.DISBURSED) {
            throw new BadRequestException("Only disbursed loans can be repaid");
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

//        BigDecimal totalRepayable = calculateTotalRepayable(loan.getLoanAmount(), loan.getInterestRate());
        BigDecimal repayAmount = request.getAmount().min(loan.getRemainingBalance());

        if (wallet.getWalletBalance().compareTo(repayAmount) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance for repayment");
        }
        cryptoUtil.validateWalletHash(wallet);

        BigDecimal updatedBalance = wallet.getWalletBalance().subtract(repayAmount);
        updateWalletBalance(wallet, updatedBalance);

        Transaction transaction = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .transactionType(TransactionType.REPAYMENT)
                .amount(repayAmount)
                .transactionStatus(TransactionStatus.SUCCESS)
                .referenceNumber(referenceUtil.generate("RPAY"))
                .description("Loan repayment for loan ID: " + loan.getLoanId())
                .build();
        transactionRepository.save(transaction);

        loan.setAmountRepaid(loan.getAmountRepaid().add(repayAmount));
        loan.setRemainingBalance(loan.getRemainingBalance().subtract(repayAmount));

        if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setLoanStatus(LoanStatus.REPAID);
        }

        // update repayment schedule
        List<RepaymentInstallment> schedule = parseSchedule(loan);
        BigDecimal remainingRepayment = repayAmount;
        for (RepaymentInstallment installment : schedule) {
            if (installment.isPaid()) {
                continue;
            }
            if (remainingRepayment.compareTo(installment.getAmount()) >= 0) {
                installment.setPaid(true);
                remainingRepayment = remainingRepayment.subtract(installment.getAmount());
            } else {
                break;
            }
        }

        try {
            loan.setRepaymentSchedule(objectMapper.writeValueAsString(schedule));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to update repayment schedule");
        }
        /*
         regenerate hash
        */
        String hash = cryptoUtil.generateLoanSignature(loan.getLoanId(), loan.getUser().getId(), loan.getLoanAmount(), loan.getTotalRepaymentAmount(),
                loan.getAmountRepaid(), loan.getRemainingBalance());
        loan.setLoanIntegrityHash(hash);
        loanRepository.save(loan);

        // Publish to RabbitMQ — consumer sends repayment success email
        notificationPublisher.publishLoanNotification(
                buildLoanMessage(loan, user, LoanNotificationMessage.EventType.LOAN_REPAID));

        log.info("Loan repaid: {}, Amount: {}", loanId, repayAmount);
        return ApiResponse.success("Loan repaid successfully", toLoanResponse(loan, parseSchedule(loan)));
    }

    @Override
    public ApiResponse<LoanResponse> getLoanById(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        return ApiResponse.success("Loan retrieved", toLoanResponse(loan, parseSchedule(loan)));
    }

    @Override
    public ApiResponse<List<LoanResponse>> getUserLoans(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<LoanResponse> loans = loanRepository.findByUser(user).stream()
                .map(l -> toLoanResponse(l, parseSchedule(l))).collect(Collectors.toList());
        return ApiResponse.success("Loans retrieved", loans);
    }

    @Override
    public ApiResponse<List<LoanResponse>> getAllLoans() {
        List<LoanResponse> loans = loanRepository.findAll().stream()
                .map(l -> toLoanResponse(l, parseSchedule(l))).collect(Collectors.toList());
        return ApiResponse.success("All loans retrieved", loans);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private List<RepaymentInstallment> generateRepaymentSchedule(BigDecimal loanAmount, BigDecimal interestRate, int durationDays) {
        BigDecimal totalRepayable = calculateTotalRepayable(loanAmount, interestRate);
        int installments = Math.min(durationDays, 4);
        BigDecimal installmentAmount = totalRepayable.divide(
                BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
        int intervalDays = durationDays / installments;

        List<RepaymentInstallment> schedule = new ArrayList<>();
        for (int i = 1; i <= installments; i++) {
            schedule.add(RepaymentInstallment.builder()
                    .installmentNumber(i)
                    .dueDate(LocalDateTime.now().plusDays((long) intervalDays * i))
                    .amount(installmentAmount)
                    .paid(false)
                    .build());
        }
        return schedule;
    }

    private BigDecimal calculateTotalRepayable(BigDecimal loanAmount, BigDecimal interestRate) {
        BigDecimal interest = loanAmount.multiply(interestRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return loanAmount.add(interest);
    }

    private List<RepaymentInstallment> parseSchedule(Loan loan) {
        try {
            if (loan.getRepaymentSchedule() == null) return List.of();
            return objectMapper.readValue(loan.getRepaymentSchedule(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RepaymentInstallment.class));
        } catch (Exception e) {
            log.warn("Could not parse repayment schedule for loan {}", loan.getLoanId());
            return List.of();
        }
    }

    private LoanNotificationMessage buildLoanMessage(Loan loan, User user, LoanNotificationMessage.EventType eventType) {
        return LoanNotificationMessage.builder()
                .loanId(loan.getLoanId().toString())
                .userId(user.getId().toString())
                .userEmail(user.getEmail())
                .userFullName(user.getFullName())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .loanStatus(loan.getLoanStatus().name())
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private LoanResponse toLoanResponse(Loan loan, List<RepaymentInstallment> schedule) {
        return LoanResponse.builder()
                .id(loan.getLoanId())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .loanDuration(loan.getLoanDurationDays())
                .totalRepayable(calculateTotalRepayable(loan.getLoanAmount(), loan.getInterestRate()))
                .loanStatus(loan.getLoanStatus().name())
                .repaymentSchedule(schedule)
                .dueDate(loan.getDueDate())
                .createdDate(loan.getCreatedDate())
                .build();
    }

    private void updateWalletBalance(Wallet wallet, BigDecimal newBalance) {
        wallet.setWalletBalance(newBalance);

        String signature = cryptoUtil.generateWalletSignature(
                wallet.getWalletId(),
                wallet.getUser().getId(),
                newBalance
        );
        wallet.setWalletBalanceHash(signature);
        walletRepository.save(wallet);
    }
}

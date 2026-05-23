package com.middleware.scheduler;

import com.middleware.entity.Loan;
import com.middleware.entity.User;
import com.middleware.entity.enums.LoanStatus;
import com.middleware.messaging.LoanNotificationMessage;
import com.middleware.messaging.NotificationPublisher;
import com.middleware.repository.LoanRepository;
import com.middleware.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanScheduler {

    private final LoanRepository loanRepository;
    private final EmailService emailService;
    private final NotificationPublisher notificationPublisher;

    /** Runs daily at midnight — marks overdue disbursed loans as DEFAULTED */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueLoans() {
        List<Loan> overdueLoans = loanRepository
                .findByLoanStatusAndDueDateBefore(LoanStatus.DISBURSED, LocalDateTime.now());

        if (!overdueLoans.isEmpty()) {
            log.info("Marking {} loan(s) as DEFAULTED", overdueLoans.size());
            overdueLoans.forEach(loan -> {loan.setLoanStatus(LoanStatus.DEFAULTED);
                loanRepository.save(loan);

                // Publish DEFAULTED event to RabbitMQ
                notificationPublisher.publishLoanNotification(
                        LoanNotificationMessage.builder()
                                .loanId(loan.getLoanId().toString())
                                .userId(loan.getUser().getId().toString())
                                .userEmail(loan.getUser().getEmail())
                                .userFullName(loan.getUser().getFullName())
                                .loanAmount(loan.getLoanAmount())
                                .interestRate(loan.getInterestRate())
                                .loanStatus(LoanStatus.DEFAULTED.name())
                                .eventType(LoanNotificationMessage.EventType.LOAN_DEFAULTED)
                                .occurredAt(LocalDateTime.now())
                                .build()
                );
                log.info("Loan {} marked as DEFAULTED", loan.getLoanId());
            });
        }
    }

    /** Runs daily at 9 AM — sends repayment reminders for loans due in 3 days */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendRepaymentReminders() {
        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
        LocalDateTime fourDaysFromNow = LocalDateTime.now().plusDays(4);

        List<Loan> dueSoonLoans = loanRepository.findByLoanStatusAndDueDateBetween(
                LoanStatus.DISBURSED, threeDaysFromNow, fourDaysFromNow);

        log.info("Sending repayment reminders for {} loan(s)", dueSoonLoans.size());

        dueSoonLoans.forEach(loan -> {
            User user = loan.getUser();
            String dueDate = loan.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            emailService.sendRepaymentReminderEmail(
                    user.getEmail(), user.getFullName(),
                    loan.getLoanAmount().toPlainString(), dueDate);
        });
    }
}

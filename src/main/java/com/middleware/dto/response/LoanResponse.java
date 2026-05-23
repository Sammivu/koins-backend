package com.middleware.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class LoanResponse {
    private UUID id;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer loanDuration;
    private BigDecimal totalRepayable;
    private String loanStatus;
    private List<RepaymentInstallment> repaymentSchedule;
    private LocalDateTime dueDate;
    private LocalDateTime createdDate;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class RepaymentInstallment {
        private Integer installmentNumber;
        private LocalDateTime dueDate;
        private BigDecimal amount;
        private boolean paid;
    }
}

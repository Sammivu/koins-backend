package com.middleware.repository;

import com.middleware.entity.Loan;
import com.middleware.entity.User;
import com.middleware.entity.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {
    List<Loan> findByUser(User user);
    List<Loan> findByLoanStatus(LoanStatus status);
    List<Loan> findByLoanStatusAndDueDateBefore(LoanStatus status, LocalDateTime date);
    List<Loan> findByLoanStatusAndDueDateBetween(LoanStatus status, LocalDateTime start, LocalDateTime end);
}

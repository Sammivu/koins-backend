package com.middleware.repository;

import com.middleware.entity.Transaction;
import com.middleware.entity.User;
import com.middleware.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByUser(User user, Pageable pageable);
    Page<Transaction> findByWallet(Wallet wallet, Pageable pageable);
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    Optional<Transaction> findByExternalReference(String externalReference);

}

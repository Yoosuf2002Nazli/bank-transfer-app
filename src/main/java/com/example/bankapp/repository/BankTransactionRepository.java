package com.example.bankapp.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findTop20ByOrderByCreatedAtDesc();

    List<BankTransaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(
            String fromAccount,
            String toAccount
    );

    List<BankTransaction> findByStatusAndAmountGreaterThanOrderByCreatedAtDesc(
            TransactionStatus status,
            BigDecimal amount
    );

    long countByStatus(TransactionStatus status);

@Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM BankTransaction t
        WHERE t.fromAccount = :accountNumber
        AND t.status = :status
        """)
        BigDecimal calculateTotalSent(
        @Param("accountNumber") String accountNumber,
        @Param("status") TransactionStatus status
);
}
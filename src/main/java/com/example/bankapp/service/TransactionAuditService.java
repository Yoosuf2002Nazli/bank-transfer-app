package com.example.bankapp.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedTransfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String description
    ) {
        BankTransaction transaction = new BankTransaction(
                fromAccount,
                toAccount,
                amount,
                TransactionStatus.FAILED,
                description
        );

        transactionRepository.save(transaction);
    }
}
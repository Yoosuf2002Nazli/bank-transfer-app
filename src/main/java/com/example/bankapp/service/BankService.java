package com.example.bankapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private static final String BANK_ACCOUNT = "999999";
    private static final BigDecimal TRANSFER_FEE_RATE =
            new BigDecimal("0.01");

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;
    private final TransactionAuditService auditService;

    public List<Customer> getCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllByOrderByFullNameAsc();
        }

        return customerRepository
                .findByFullNameContainingIgnoreCaseOrderByFullNameAsc(
                        keyword.trim()
                );
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByFullNameAsc();
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<BankTransaction> getAccountTransactions(
            String accountNumber
    ) {
        getCustomer(accountNumber);

        return transactionRepository
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(
                        accountNumber,
                        accountNumber
                );
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal minimum) {
        return transactionRepository
                .findByStatusAndAmountGreaterThanOrderByCreatedAtDesc(
                        TransactionStatus.SUCCESS,
                        minimum
                );
    }

    public BigDecimal getTotalBalance() {
        return customerRepository.calculateTotalBalance();
    }

    public long getSuccessfulTransferCount() {
        return transactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long getFailedTransferCount() {
        return transactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        BigDecimal total = transactionRepository.calculateTotalSent(
                accountNumber,
                TransactionStatus.SUCCESS
        );

        if (total == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return total
                .multiply(BigDecimal.ONE.add(TRANSFER_FEE_RATE))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public Customer addCustomer(Customer customer) {
        if (customerRepository.existsByAccountNumber(
                customer.getAccountNumber()
        )) {
            throw new BankException(
                    "Account number already exists: "
                            + customer.getAccountNumber()
            );
        }

        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new BankException(
                    "Email already exists: " + customer.getEmail()
            );
        }

        if (customer.getBalance() == null) {
            customer.setBalance(BigDecimal.ZERO);
        }

        customer.setBalance(
                customer.getBalance().setScale(2, RoundingMode.HALF_UP)
        );

        return customerRepository.save(customer);
    }

    @Transactional
    public void deposit(String accountNumber, BigDecimal amount) {
        validatePositiveAmount(amount);

        Customer customer = getCustomer(accountNumber);
        customer.setBalance(customer.getBalance().add(amount));
        customerRepository.save(customer);

        transactionRepository.save(new BankTransaction(
                BANK_ACCOUNT,
                accountNumber,
                amount,
                TransactionStatus.SUCCESS,
                "Cash deposit"
        ));
    }

    @Transactional
    public void withdraw(String accountNumber, BigDecimal amount) {
        validatePositiveAmount(amount);

        Customer customer = getCustomer(accountNumber);
        ensureSufficientBalance(customer, amount);

        customer.setBalance(customer.getBalance().subtract(amount));
        customerRepository.save(customer);

        transactionRepository.save(new BankTransaction(
                accountNumber,
                BANK_ACCOUNT,
                amount,
                TransactionStatus.SUCCESS,
                "Cash withdrawal"
        ));
    }

     @Transactional(rollbackFor = Exception.class)
    public void transfer(TransferRequestData request) {
        validatePositiveAmount(request.amount());

        if (request.fromAccount().equals(request.toAccount())) {
            throw new BankException(
                    "Sender and receiver accounts must be different"
            );
        }

        Customer sender = getCustomer(request.fromAccount());
        Customer receiver = getCustomer(request.toAccount());

        BigDecimal fee = request.amount()
                .multiply(TRANSFER_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDebit = request.amount().add(fee);

        ensureSufficientBalance(sender, totalDebit);

        sender.setBalance(sender.getBalance().subtract(totalDebit));
        receiver.setBalance(receiver.getBalance().add(request.amount()));

        Customer bank = getCustomer(BANK_ACCOUNT);
        bank.setBalance(bank.getBalance().add(fee));

        customerRepository.save(sender);
        customerRepository.save(receiver);
        customerRepository.save(bank);

        if (request.simulateFailure()) {
            auditService.recordFailedTransfer(
                    request.fromAccount(),
                    request.toAccount(),
                    request.amount(),
                    "Simulated failure after withdrawal"
            );

            throw new BankException(
                    "Simulated failure: transfer was rolled back"
            );
        }

        transactionRepository.save(new BankTransaction(
                request.fromAccount(),
                request.toAccount(),
                request.amount(),
                TransactionStatus.SUCCESS,
                "Transfer completed; fee: LKR " + fee
        ));
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than zero");
        }
    }

    private void ensureSufficientBalance(
            Customer customer,
            BigDecimal amount
    ) {
        if (customer.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    customer.getAccountNumber()
            );
        }
    }

    public record TransferRequestData(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure
    ) {
    }
}
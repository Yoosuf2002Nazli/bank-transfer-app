package com.example.bankapp.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankapp.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);

    List<Customer> findByFullNameContainingIgnoreCaseOrderByFullNameAsc(
            String keyword
    );

    List<Customer> findAllByOrderByFullNameAsc();

    default BigDecimal calculateTotalBalance() {
        return findAll()
                .stream()
                .map(Customer::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
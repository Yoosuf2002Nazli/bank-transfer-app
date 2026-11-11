package com.example.bankapp.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }

        Customer bank = createCustomer(
                "Bank Reserve Account",
                "bank@bankapp.com",
                "999999",
                "0.00"
        );

        Customer customerOne = createCustomer(
                "Nuwan Jayasinghe",
                "nuwan@example.com",
                "100001",
                "50000.00"
        );

        Customer customerTwo = createCustomer(
                "Amali Perera",
                "amali@example.com",
                "100002",
                "35000.00"
        );

        Customer customerThree = createCustomer(
                "Kasun Fernando",
                "kasun@example.com",
                "100003",
                "25000.00"
        );

        customerRepository.saveAll(List.of(
                bank,
                customerOne,
                customerTwo,
                customerThree
        ));
    }

    private Customer createCustomer(
            String fullName,
            String email,
            String accountNumber,
            String balance
    ) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setAccountNumber(accountNumber);
        customer.setBalance(new BigDecimal(balance));
        return customer;
    }
}
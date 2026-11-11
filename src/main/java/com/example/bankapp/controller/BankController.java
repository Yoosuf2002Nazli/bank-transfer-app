package com.example.bankapp.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        model.addAttribute("customers", bankService.getCustomers(keyword));
        model.addAttribute("allCustomers", bankService.getAllCustomers());
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("totalCustomers",
                bankService.getAllCustomers().size());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount",
                bankService.getSuccessfulTransferCount());
        model.addAttribute("failedCount",
                bankService.getFailedTransferCount());
        model.addAttribute("keyword", keyword == null ? "" : keyword);

        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }

        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }

        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.customer",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("customer", customer);
            return "redirect:/";
        }

        try {
            bankService.addCustomer(customer);
            redirectAttributes.addFlashAttribute(
                    "success",
                "Customer added successfully"
            );
        } catch (BankException exception) {
        redirectAttributes.addFlashAttribute(
                "error",
                exception.getMessage()
        );
        }

        return "redirect:/";
        }

    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest")
            TransferRequest transferRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.transferRequest",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute(
                    "transferRequest",
                    transferRequest
            );
            return "redirect:/";
        }

        try {
            bankService.transfer(new BankService.TransferRequestData(
                    transferRequest.getFromAccount(),
                    transferRequest.getToAccount(),
                    transferRequest.getAmount(),
                    transferRequest.isSimulateFailure()
            ));

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Transfer completed successfully"
            );
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Deposit completed successfully"
            );
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Withdrawal completed successfully"
            );
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/withdraw";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model
    ) {
        Customer customer = bankService.getCustomer(accountNumber);

        model.addAttribute("customer", customer);
        model.addAttribute(
                "transactions",
                bankService.getAccountTransactions(accountNumber)
        );
        model.addAttribute(
                "totalSent",
                bankService.getTotalSent(accountNumber)
        );

        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model
    ) {
        model.addAttribute("min", min);
        model.addAttribute(
                "transfers",
                bankService.getLargeTransfers(min)
        );

        return "large-transfers";
    }
}
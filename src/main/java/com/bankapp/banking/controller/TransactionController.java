package com.bankapp.banking.controller;

import com.bankapp.banking.dto.TransactionResponse;
import com.bankapp.banking.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsForAccount(accountId));
    }
}

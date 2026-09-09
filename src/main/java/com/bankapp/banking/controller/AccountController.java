package com.bankapp.banking.controller;

import com.bankapp.banking.dto.AccountResponse;
import com.bankapp.banking.dto.CreateAccountRequest;
import com.bankapp.banking.dto.UpdateAccountRequest;
import com.bankapp.banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        return ResponseEntity.ok(accountService.getMyAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id,
                                                           @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable Long id) {
        BigDecimal balance = accountService.getBalance(id);
        return ResponseEntity.ok(Map.of("accountId", id, "balance", balance));
    }

    // Soft-delete: closes the account rather than removing the row, preserving
    // the transaction history's foreign key integrity.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> closeAccount(@PathVariable Long id) {
        accountService.closeAccount(id);
        return ResponseEntity.noContent().build();
    }
}

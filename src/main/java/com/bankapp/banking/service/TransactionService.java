package com.bankapp.banking.service;

import com.bankapp.banking.dto.TransactionResponse;
import com.bankapp.banking.entity.User;
import com.bankapp.banking.entity.enums.Role;
import com.bankapp.banking.exception.AccessDeniedExceptionCustom;
import com.bankapp.banking.exception.ResourceNotFoundException;
import com.bankapp.banking.repository.AccountRepository;
import com.bankapp.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final SecurityUtils securityUtils;

    public List<TransactionResponse> getTransactionsForAccount(Long accountId) {
        User currentUser = securityUtils.getCurrentUser();

        // Ownership check pushed into the repository query - never touches
        // Account.user, so it can't trigger a LazyInitializationException,
        // and a user can't view another user's history by guessing an id.
        if (currentUser.getRole() != Role.ROLE_ADMIN) {
            boolean owns = accountRepository.existsByIdAndUserId(accountId, currentUser.getId());
            if (!owns) {
                if (!accountRepository.existsById(accountId)) {
                    throw new ResourceNotFoundException("Account not found with id: " + accountId);
                }
                throw new AccessDeniedExceptionCustom("You do not have permission to view this account's transactions");
            }
        } else if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }

        return transactionRepository
                .findByAccountIdWithAccounts(accountId)
                .stream().map(TransactionResponse::fromEntity).collect(Collectors.toList());
    }
}

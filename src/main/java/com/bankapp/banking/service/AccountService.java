package com.bankapp.banking.service;

import com.bankapp.banking.dto.AccountResponse;
import com.bankapp.banking.dto.CreateAccountRequest;
import com.bankapp.banking.dto.UpdateAccountRequest;
import com.bankapp.banking.entity.Account;
import com.bankapp.banking.entity.User;
import com.bankapp.banking.entity.enums.AccountStatus;
import com.bankapp.banking.entity.enums.Role;
import com.bankapp.banking.exception.AccessDeniedExceptionCustom;
import com.bankapp.banking.exception.ResourceNotFoundException;
import com.bankapp.banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Note on lazy loading: Account.user stays FetchType.LAZY (see Account
 * entity) - it is never switched to EAGER. Every method here that needs
 * ownership information or the owner's name gets it via a repository-level
 * query (findByIdAndUserId / *WithUser JOIN FETCH variants) instead of
 * calling account.getUser() on an entity that might outlive its session.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final SecurityUtils securityUtils;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Account account = new Account();
        account.setUser(currentUser);
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getInitialDeposit() == null ? BigDecimal.ZERO : request.getInitialDeposit());

        Account saved = accountRepository.save(account);
        // currentUser is the actual loaded entity (not a lazy proxy), so this is safe.
        return AccountResponse.fromEntityWithOwner(saved, currentUser.getFullName());
    }

    public List<AccountResponse> getMyAccounts() {
        User currentUser = securityUtils.getCurrentUser();
        // JOIN FETCH so ownerName is populated without a second lazy-load query.
        return accountRepository.findByUserIdWithUser(currentUser.getId())
                .stream()
                .map(a -> AccountResponse.fromEntityWithOwner(a, a.getUser().getFullName()))
                .collect(Collectors.toList());
    }

    public AccountResponse getAccountById(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Account account = fetchWithOwnershipCheck(id, currentUser);
        return AccountResponse.fromEntityWithOwner(account, account.getUser().getFullName());
    }

    public BigDecimal getBalance(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Account account = fetchOwnedAccount(id, currentUser);
        return account.getBalance();
    }

    @Transactional
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Account account = fetchOwnedAccount(id, currentUser);

        if (request.getStatus() != null) {
            account.setStatus(request.getStatus());
        }
        Account saved = accountRepository.save(account);
        return AccountResponse.fromEntity(saved);
    }

    /**
     * Banking systems don't hard-delete accounts (that would break the
     * transaction audit trail's foreign keys); closing sets status=CLOSED
     * instead. Refuses to close an account that still holds a balance.
     */
    @Transactional
    public void closeAccount(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Account account = fetchOwnedAccount(id, currentUser);

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Cannot close an account with a positive balance. Please transfer out the remaining funds first.");
        }
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    // ------------------------------------------------------------------
    // Ownership resolution helpers
    // ------------------------------------------------------------------

    /** Fetch an account for read/write where the caller does NOT need owner details (no JOIN FETCH needed). */
    private Account fetchOwnedAccount(Long id, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return accountRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        }
        return accountRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> resolveNotFoundOrForbidden(id));
    }

    /** Fetch an account together with its owner (JOIN FETCH) for building an AccountResponse. */
    private Account fetchWithOwnershipCheck(Long id, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return accountRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        }
        return accountRepository.findByIdAndUserIdWithUser(id, currentUser.getId())
                .orElseThrow(() -> resolveNotFoundOrForbidden(id));
    }

    /**
     * Distinguishes "doesn't exist" (404) from "exists but belongs to someone
     * else" (403) without ever touching account.getUser() - existsById alone
     * tells us which case we're in.
     */
    private RuntimeException resolveNotFoundOrForbidden(Long id) {
        if (accountRepository.existsById(id)) {
            return new AccessDeniedExceptionCustom("You do not have permission to access this account");
        }
        return new ResourceNotFoundException("Account not found with id: " + id);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = 1000000000L + (long) (RANDOM.nextDouble() * 8999999999L);
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}

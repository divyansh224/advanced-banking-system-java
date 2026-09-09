package com.bankapp.banking.dto;

import com.bankapp.banking.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;
    private String ownerName;

    /**
     * Deliberately does NOT touch account.getUser() - Account.user is
     * FetchType.LAZY, and calling it here would tie this DTO's safety to
     * whether the caller happened to load the account with a JOIN FETCH.
     * Use fromEntityWithOwner(...) when the owner's name is available and
     * needed in the response.
     */
    public static AccountResponse fromEntity(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountType(account.getAccountType().name());
        dto.setBalance(account.getBalance());
        dto.setStatus(account.getStatus().name());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }

    /** Use when the account was fetched with its User already joined/initialized. */
    public static AccountResponse fromEntityWithOwner(Account account, String ownerName) {
        AccountResponse dto = fromEntity(account);
        dto.setOwnerName(ownerName);
        return dto;
    }
}

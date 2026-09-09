package com.bankapp.banking.dto;

import com.bankapp.banking.entity.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    private BigDecimal initialDeposit = BigDecimal.ZERO;
}

package com.bankapp.banking.dto;

import com.bankapp.banking.entity.enums.AccountStatus;
import lombok.Data;

@Data
public class UpdateAccountRequest {
    private AccountStatus status;
}

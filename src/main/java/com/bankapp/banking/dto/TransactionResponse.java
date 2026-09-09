package com.bankapp.banking.dto;

import com.bankapp.banking.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionRef;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String type;
    private String status;
    private String description;
    private LocalDateTime timestamp;

    public static TransactionResponse fromEntity(Transaction txn) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(txn.getId());
        dto.setTransactionRef(txn.getTransactionRef());
        dto.setFromAccountNumber(txn.getFromAccount() != null ? txn.getFromAccount().getAccountNumber() : null);
        dto.setToAccountNumber(txn.getToAccount() != null ? txn.getToAccount().getAccountNumber() : null);
        dto.setAmount(txn.getAmount());
        dto.setType(txn.getType().name());
        dto.setStatus(txn.getStatus().name());
        dto.setDescription(txn.getDescription());
        dto.setTimestamp(txn.getTimestamp());
        return dto;
    }
}

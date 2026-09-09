package com.bankapp.banking.dto;

import com.bankapp.banking.entity.FraudAnalysis;
import com.bankapp.banking.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisResponse {
    private Long transactionId;
    private String transactionRef;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String transactionStatus;

    private Integer riskScore;
    private String riskLevel;
    private String fraudReason;
    private String reviewStatus;
    private LocalDateTime reviewedAt;
    private String reviewedBy;

    /** txn's fromAccount/toAccount must already be initialized (JOIN FETCH) - never touched lazily here. */
    public static FraudAnalysisResponse fromEntities(Transaction txn, FraudAnalysis analysis) {
        FraudAnalysisResponse dto = new FraudAnalysisResponse();
        dto.setTransactionId(txn.getId());
        dto.setTransactionRef(txn.getTransactionRef());
        dto.setFromAccountNumber(txn.getFromAccount() != null ? txn.getFromAccount().getAccountNumber() : null);
        dto.setToAccountNumber(txn.getToAccount() != null ? txn.getToAccount().getAccountNumber() : null);
        dto.setAmount(txn.getAmount());
        dto.setTimestamp(txn.getTimestamp());
        dto.setTransactionStatus(txn.getStatus().name());

        dto.setRiskScore(analysis.getRiskScore());
        dto.setRiskLevel(analysis.getRiskLevel().name());
        dto.setFraudReason(analysis.getFraudReason());
        dto.setReviewStatus(analysis.getReviewStatus().name());
        dto.setReviewedAt(analysis.getReviewedAt());
        dto.setReviewedBy(analysis.getReviewedBy());
        return dto;
    }
}

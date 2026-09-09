package com.bankapp.banking.service;

import com.bankapp.banking.dto.FraudAnalysisResponse;
import com.bankapp.banking.entity.Account;
import com.bankapp.banking.entity.FraudAnalysis;
import com.bankapp.banking.entity.Transaction;
import com.bankapp.banking.entity.enums.AccountStatus;
import com.bankapp.banking.entity.enums.FraudReviewStatus;
import com.bankapp.banking.entity.enums.TransactionStatus;
import com.bankapp.banking.exception.FraudReviewException;
import com.bankapp.banking.exception.InsufficientBalanceException;
import com.bankapp.banking.exception.InvalidAccountException;
import com.bankapp.banking.exception.ResourceNotFoundException;
import com.bankapp.banking.repository.AccountRepository;
import com.bankapp.banking.repository.FraudAnalysisRepository;
import com.bankapp.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only workflow for reviewing transfers that FraudRiskEngine flagged
 * as HIGH/CRITICAL risk (see TransferService). Nothing here is reachable by
 * a regular user - endpoint-level ROLE_ADMIN protection lives in
 * SecurityConfig / AdminFraudController.
 */
@Service
@RequiredArgsConstructor
public class FraudAdminService {

    private final TransactionRepository transactionRepository;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final AccountRepository accountRepository;
    private final SecurityUtils securityUtils;

    public List<FraudAnalysisResponse> getPendingReviews() {
        return transactionRepository.findPendingFraudReview().stream()
                .map(txn -> FraudAnalysisResponse.fromEntities(txn, txn.getFraudAnalysis()))
                .collect(Collectors.toList());
    }

    /**
     * Approves a flagged transfer: re-validates everything that could have
     * changed since the transfer was first flagged (account status, balance),
     * then actually moves the money and marks the transaction SUCCESS.
     * Uses the same pessimistic-lock, consistent-order pattern as
     * TransferService.transfer() for exactly the same reasons.
     */
    @Transactional
    public FraudAnalysisResponse approve(Long transactionId) {
        Transaction transaction = transactionRepository.findByIdWithAccounts(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        FraudAnalysis analysis = fraudAnalysisRepository.findByTransaction_Id(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("No fraud analysis found for transaction " + transactionId));

        requirePendingReview(transaction, analysis);

        String fromNumber = transaction.getFromAccount().getAccountNumber();
        String toNumber = transaction.getToAccount().getAccountNumber();
        String first = fromNumber.compareTo(toNumber) < 0 ? fromNumber : toNumber;
        String second = first.equals(fromNumber) ? toNumber : fromNumber;
        accountRepository.findByAccountNumberForUpdate(first);
        accountRepository.findByAccountNumberForUpdate(second);

        Account fromAccount = accountRepository.findByAccountNumber(fromNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found: " + fromNumber));
        Account toAccount = accountRepository.findByAccountNumber(toNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found: " + toNumber));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Sender account is no longer active");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Receiver account is no longer active");
        }

        BigDecimal amount = transaction.getAmount();
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            markReviewed(analysis, FraudReviewStatus.REJECTED);
            throw new InsufficientBalanceException("Sender no longer has sufficient balance for this transfer");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transaction.setStatus(TransactionStatus.SUCCESS);
        Transaction saved = transactionRepository.save(transaction);

        FraudAnalysis savedAnalysis = markReviewed(analysis, FraudReviewStatus.APPROVED);
        return FraudAnalysisResponse.fromEntities(saved, savedAnalysis);
    }

    /** Rejects a flagged transfer: no money ever moved, so this just finalizes the transaction as FAILED. */
    @Transactional
    public FraudAnalysisResponse reject(Long transactionId) {
        Transaction transaction = transactionRepository.findByIdWithAccounts(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        FraudAnalysis analysis = fraudAnalysisRepository.findByTransaction_Id(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("No fraud analysis found for transaction " + transactionId));

        requirePendingReview(transaction, analysis);

        transaction.setStatus(TransactionStatus.FAILED);
        Transaction saved = transactionRepository.save(transaction);

        FraudAnalysis savedAnalysis = markReviewed(analysis, FraudReviewStatus.REJECTED);
        return FraudAnalysisResponse.fromEntities(saved, savedAnalysis);
    }

    private void requirePendingReview(Transaction transaction, FraudAnalysis analysis) {
        if (transaction.getStatus() != TransactionStatus.PENDING
                || analysis.getReviewStatus() != FraudReviewStatus.PENDING_REVIEW) {
            throw new FraudReviewException("This transaction has already been reviewed (status: "
                    + analysis.getReviewStatus() + ")");
        }
    }

    private FraudAnalysis markReviewed(FraudAnalysis analysis, FraudReviewStatus outcome) {
        analysis.setReviewStatus(outcome);
        analysis.setReviewedAt(LocalDateTime.now());
        analysis.setReviewedBy(securityUtils.getCurrentUser().getUsername());
        return fraudAnalysisRepository.save(analysis);
    }
}

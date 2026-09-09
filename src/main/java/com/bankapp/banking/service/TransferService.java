package com.bankapp.banking.service;

import com.bankapp.banking.dto.TransactionResponse;
import com.bankapp.banking.dto.TransferRequest;
import com.bankapp.banking.entity.Account;
import com.bankapp.banking.entity.FraudAnalysis;
import com.bankapp.banking.entity.Transaction;
import com.bankapp.banking.entity.User;
import com.bankapp.banking.entity.enums.AccountStatus;
import com.bankapp.banking.entity.enums.FraudReviewStatus;
import com.bankapp.banking.entity.enums.TransactionStatus;
import com.bankapp.banking.entity.enums.TransactionType;
import com.bankapp.banking.exception.AccessDeniedExceptionCustom;
import com.bankapp.banking.exception.InsufficientBalanceException;
import com.bankapp.banking.exception.InvalidAccountException;
import com.bankapp.banking.exception.ResourceNotFoundException;
import com.bankapp.banking.fraud.FraudAssessment;
import com.bankapp.banking.fraud.FraudRiskEngine;
import com.bankapp.banking.repository.AccountRepository;
import com.bankapp.banking.repository.FraudAnalysisRepository;
import com.bankapp.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Handles money transfers between two accounts.
 *
 * The whole operation (validate -> risk-score -> debit -> credit -> record
 * transaction) runs inside a single @Transactional boundary, so if any step
 * fails the entire operation is rolled back atomically - the sender is never
 * debited without the receiver being credited, and vice versa.
 *
 * Every transfer is risk-scored by FraudRiskEngine BEFORE any balance is
 * touched. LOW/MEDIUM risk transfers proceed immediately (their score is
 * still stored, for analytics). HIGH/CRITICAL risk transfers are NOT
 * completed automatically: the transaction is recorded as PENDING with no
 * money moved, and it sits in the admin fraud-review queue until an admin
 * approves or rejects it (see FraudAdminService).
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final FraudRiskEngine fraudRiskEngine;
    private final SecurityUtils securityUtils;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transfer(TransferRequest request) {

        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new InvalidAccountException("Sender and receiver accounts must be different");
        }

        User currentUser = securityUtils.getCurrentUser();

        // Lock both rows for the duration of the transaction to prevent race
        // conditions where two concurrent transfers could double-spend the
        // same balance. Locking in a consistent (account number) order also
        // avoids classic deadlocks between two simultaneous opposite transfers.
        String first = request.getFromAccountNumber().compareTo(request.getToAccountNumber()) < 0
                ? request.getFromAccountNumber() : request.getToAccountNumber();
        String second = first.equals(request.getFromAccountNumber())
                ? request.getToAccountNumber() : request.getFromAccountNumber();

        accountRepository.findByAccountNumberForUpdate(first);
        accountRepository.findByAccountNumberForUpdate(second);

        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found: " + request.getFromAccountNumber()));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found: " + request.getToAccountNumber()));

        // Ownership check via a repository query rather than fromAccount.getUser().getId() -
        // Account.user is intentionally FetchType.LAZY, and this avoids depending on it
        // ever being loaded/initialized.
        boolean isAdmin = currentUser.getRole().name().equals("ROLE_ADMIN");
        boolean isOwner = accountRepository.existsByIdAndUserId(fromAccount.getId(), currentUser.getId());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedExceptionCustom("You can only transfer funds from your own account");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Sender account is not active");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Receiver account is not active");
        }

        BigDecimal amount = request.getAmount();

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            // Record the failed attempt for a complete audit trail, then reject.
            Transaction failed = new Transaction();
            failed.setFromAccount(fromAccount);
            failed.setToAccount(toAccount);
            failed.setAmount(amount);
            failed.setType(TransactionType.TRANSFER);
            failed.setStatus(TransactionStatus.FAILED);
            failed.setDescription(request.getDescription() != null ? request.getDescription() : "Insufficient funds");
            transactionRepository.save(failed);

            throw new InsufficientBalanceException("Insufficient balance in sender account");
        }

        // ---- Fraud / risk scoring, BEFORE any balance is touched ----
        FraudAssessment assessment = fraudRiskEngine.evaluate(fromAccount, amount);

        if (assessment.flagged()) {
            // HIGH/CRITICAL risk: do NOT move money. Record the transaction as
            // PENDING and park it in the admin review queue.
            Transaction pending = new Transaction();
            pending.setFromAccount(fromAccount);
            pending.setToAccount(toAccount);
            pending.setAmount(amount);
            pending.setType(TransactionType.TRANSFER);
            pending.setStatus(TransactionStatus.PENDING);
            pending.setDescription(request.getDescription());
            Transaction savedPending = transactionRepository.save(pending);

            saveFraudAnalysis(savedPending, assessment, FraudReviewStatus.PENDING_REVIEW);

            return TransactionResponse.fromEntity(savedPending);
        }

        // LOW/MEDIUM risk: proceed immediately. Debit sender, credit receiver.
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.getDescription());

        Transaction saved = transactionRepository.save(transaction);

        // Stored even though it didn't block anything - gives the fraud
        // system a full population of scores to analyze later, not just
        // the flagged outliers.
        saveFraudAnalysis(saved, assessment, FraudReviewStatus.NOT_FLAGGED);

        return TransactionResponse.fromEntity(saved);
    }

    private void saveFraudAnalysis(Transaction transaction, FraudAssessment assessment, FraudReviewStatus reviewStatus) {
        FraudAnalysis analysis = new FraudAnalysis();
        analysis.setTransaction(transaction);
        analysis.setRiskScore(assessment.score());
        analysis.setRiskLevel(assessment.level());
        analysis.setFraudFlag(assessment.flagged());
        analysis.setFraudReason(assessment.reason());
        analysis.setReviewStatus(reviewStatus);
        fraudAnalysisRepository.save(analysis);
    }
}

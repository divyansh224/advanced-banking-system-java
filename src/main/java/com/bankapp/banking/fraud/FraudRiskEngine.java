package com.bankapp.banking.fraud;

import com.bankapp.banking.entity.Account;
import com.bankapp.banking.entity.Transaction;
import com.bankapp.banking.entity.enums.RiskLevel;
import com.bankapp.banking.entity.enums.TransactionStatus;
import com.bankapp.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based fraud/risk-scoring engine. Runs BEFORE a transfer's balances are
 * touched (see TransferService) and never blocks or allows a transfer on its
 * own - it only produces a score; TransferService decides what to do with it.
 *
 * Each rule below is independent and additive, so new rules can be dropped
 * in without touching the others. Thresholds are kept as named constants
 * rather than magic numbers so they're easy to find and tune.
 *
 * Score bands (see RiskLevel):
 *   0-30    LOW       - proceeds automatically
 *   31-60   MEDIUM    - proceeds automatically, recorded for analytics
 *   61-80   HIGH      - blocked, sent to admin review
 *   81-100  CRITICAL  - blocked, sent to admin review
 */
@Component
@RequiredArgsConstructor
public class FraudRiskEngine {

    private final TransactionRepository transactionRepository;

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal VERY_LARGE_AMOUNT_THRESHOLD = new BigDecimal("20000");

    private static final Duration VELOCITY_WINDOW = Duration.ofMinutes(10);
    private static final int VELOCITY_SOFT_LIMIT = 3;
    private static final int VELOCITY_HARD_LIMIT = 6;

    private static final Duration FAILURE_WINDOW = Duration.ofHours(24);
    private static final int REPEATED_FAILURE_LIMIT = 3;

    private static final int MIN_HISTORY_FOR_DEVIATION_CHECK = 5;
    private static final BigDecimal DEVIATION_MULTIPLIER = new BigDecimal("3");

    public FraudAssessment evaluate(Account fromAccount, BigDecimal amount) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Rule 1: unusually large transaction amount (tiered)
        if (amount.compareTo(VERY_LARGE_AMOUNT_THRESHOLD) > 0) {
            score += 40;
            reasons.add("Unusually high transaction amount");
        } else if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            score += 20;
            reasons.add("Transaction amount above typical range");
        }

        // Rule 2: transaction velocity - many transfers in a short window
        long recentCount = transactionRepository.countByFromAccount_IdAndTimestampAfter(
                fromAccount.getId(), now.minus(VELOCITY_WINDOW));
        if (recentCount >= VELOCITY_HARD_LIMIT) {
            score += 35;
            reasons.add("Abnormally high transaction frequency (" + recentCount + " transfers in "
                    + VELOCITY_WINDOW.toMinutes() + " min)");
        } else if (recentCount >= VELOCITY_SOFT_LIMIT) {
            score += 15;
            reasons.add("Multiple transactions within a short period (" + recentCount + " in "
                    + VELOCITY_WINDOW.toMinutes() + " min)");
        }

        // Rule 3: large deviation from this account's own typical transfer size
        List<Transaction> recentSuccessful = transactionRepository
                .findTop20ByFromAccount_IdAndStatusOrderByTimestampDesc(fromAccount.getId(), TransactionStatus.SUCCESS);
        if (recentSuccessful.size() >= MIN_HISTORY_FOR_DEVIATION_CHECK) {
            BigDecimal avg = average(recentSuccessful);
            if (avg.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(avg.multiply(DEVIATION_MULTIPLIER)) > 0) {
                score += 20;
                reasons.add("Large deviation from this account's usual transaction size (avg ~"
                        + avg.setScale(2, RoundingMode.HALF_UP) + ")");
            }
        }

        // Rule 4: repeated failed transactions recently (possible probing/testing behavior)
        long recentFailures = transactionRepository.countByFromAccount_IdAndStatusAndTimestampAfter(
                fromAccount.getId(), TransactionStatus.FAILED, now.minus(FAILURE_WINDOW));
        if (recentFailures >= REPEATED_FAILURE_LIMIT) {
            score += 20;
            reasons.add("Repeated failed transaction attempts (" + recentFailures + " in the last 24h)");
        }

        score = Math.min(score, 100);
        RiskLevel level = classify(score);
        boolean flagged = level == RiskLevel.HIGH || level == RiskLevel.CRITICAL;
        String reasonText = reasons.isEmpty() ? "No unusual activity detected" : String.join("; ", reasons);

        return new FraudAssessment(score, level, flagged, reasonText);
    }

    private RiskLevel classify(int score) {
        if (score >= 81) return RiskLevel.CRITICAL;
        if (score >= 61) return RiskLevel.HIGH;
        if (score >= 31) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private BigDecimal average(List<Transaction> txns) {
        BigDecimal sum = txns.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(txns.size()), 2, RoundingMode.HALF_UP);
    }
}

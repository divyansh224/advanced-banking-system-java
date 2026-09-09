package com.bankapp.banking.repository;

import com.bankapp.banking.entity.Transaction;
import com.bankapp.banking.entity.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * fromAccount/toAccount are FetchType.LAZY on Transaction. This method is
     * called from a non-@Transactional service method, so by the time control
     * returns to the caller the loading session is already closed - touching
     * txn.getFromAccount().getAccountNumber() afterwards would throw
     * LazyInitializationException. JOIN FETCH both associations up front so
     * the returned entities are fully usable (and this also avoids an N+1
     * query per transaction row).
     */
    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.fromAccount " +
           "LEFT JOIN FETCH t.toAccount " +
           "WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId) " +
           "ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountIdWithAccounts(@Param("accountId") Long accountId);

    // ------------------------------------------------------------------
    // Signals consumed by fraud.FraudRiskEngine. Each is a narrow, single-
    // purpose count/lookup rather than pulling whole transaction lists into
    // memory, and none of them touch the lazy `user` association.
    // ------------------------------------------------------------------

    /** Velocity check: how many transfers has this account SENT very recently. */
    long countByFromAccount_IdAndTimestampAfter(Long accountId, LocalDateTime after);

    /** Repeated-failure check: how many FAILED transfers has this account attempted recently. */
    long countByFromAccount_IdAndStatusAndTimestampAfter(Long accountId, TransactionStatus status, LocalDateTime after);

    /** Behavioral-deviation check: this account's recent successful outgoing amounts, to compare against. */
    List<Transaction> findTop20ByFromAccount_IdAndStatusOrderByTimestampDesc(Long accountId, TransactionStatus status);

    // ------------------------------------------------------------------
    // Admin fraud review queue.
    // ------------------------------------------------------------------

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.fromAccount " +
           "LEFT JOIN FETCH t.toAccount " +
           "JOIN FETCH t.fraudAnalysis f " +
           "WHERE f.reviewStatus = com.bankapp.banking.entity.enums.FraudReviewStatus.PENDING_REVIEW " +
           "ORDER BY t.timestamp ASC")
    List<Transaction> findPendingFraudReview();

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.fromAccount " +
           "LEFT JOIN FETCH t.toAccount " +
           "WHERE t.id = :id")
    java.util.Optional<Transaction> findByIdWithAccounts(@Param("id") Long id);
}

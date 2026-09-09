package com.bankapp.banking.entity;

import com.bankapp.banking.entity.enums.TransactionStatus;
import com.bankapp.banking.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 40)
    private String transactionRef = UUID.randomUUID().toString();

    // Nullable because a DEPOSIT has no "from" account.
    // Association fields are excluded from Lombok's generated toString/equals/hashCode
    // (see User.accounts for the full rationale) - here it also avoids initializing a
    // lazy proxy just to log or compare a Transaction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account fromAccount;

    // Nullable because a WITHDRAWAL has no "to" account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    // Inverse side of FraudAnalysis.transaction - read-only navigation, the
    // FK/ownership lives on FraudAnalysis. Lazy, and only ever populated via
    // an explicit JOIN FETCH (see TransactionRepository.findPendingFraudReview).
    // Excluded from toString/equals/hashCode: without this, Transaction<->FraudAnalysis
    // is a circular reference that would recurse infinitely (StackOverflowError) the
    // first time either entity's toString()/equals() ran.
    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FraudAnalysis fraudAnalysis;
}

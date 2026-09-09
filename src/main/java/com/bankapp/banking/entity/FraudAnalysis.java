package com.bankapp.banking.entity;

import com.bankapp.banking.entity.enums.FraudReviewStatus;
import com.bankapp.banking.entity.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One risk assessment per Transaction, produced by fraud.FraudRiskEngine
 * BEFORE any money moves. Stored even for LOW/MEDIUM risk transfers so the
 * full population of scores is available for admin analytics later, not
 * just the flagged ones.
 */
@Entity
@Table(name = "fraud_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    // See Transaction.fraudAnalysis for why this is excluded from generated toString/equals/hashCode.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Transaction transaction;

    @Column(nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private boolean fraudFlag;

    @Column(length = 500)
    private String fraudReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudReviewStatus reviewStatus = FraudReviewStatus.NOT_FLAGGED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @Column(length = 50)
    private String reviewedBy;
}

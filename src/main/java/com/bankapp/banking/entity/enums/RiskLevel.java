package com.bankapp.banking.entity.enums;

/**
 * Risk bands produced by the fraud-scoring engine (see fraud.FraudRiskEngine).
 *   0-30   LOW        - proceeds automatically
 *   31-60  MEDIUM      - proceeds automatically, but is recorded for analytics
 *   61-80  HIGH        - blocked, sent to admin review
 *   81-100 CRITICAL    - blocked, sent to admin review
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

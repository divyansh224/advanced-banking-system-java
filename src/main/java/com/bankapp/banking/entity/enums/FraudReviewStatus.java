package com.bankapp.banking.entity.enums;

public enum FraudReviewStatus {
    NOT_FLAGGED,     // risk score was LOW/MEDIUM - never needed review
    PENDING_REVIEW,  // risk score was HIGH/CRITICAL - awaiting an admin decision
    APPROVED,        // admin approved - funds were released
    REJECTED         // admin rejected - funds were never moved
}

package com.bankapp.banking.fraud;

import com.bankapp.banking.entity.enums.RiskLevel;

/** Immutable result of one FraudRiskEngine.evaluate(...) call. */
public record FraudAssessment(int score, RiskLevel level, boolean flagged, String reason) {
}

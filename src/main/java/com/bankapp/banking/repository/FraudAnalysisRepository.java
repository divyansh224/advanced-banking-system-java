package com.bankapp.banking.repository;

import com.bankapp.banking.entity.FraudAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FraudAnalysisRepository extends JpaRepository<FraudAnalysis, Long> {
    Optional<FraudAnalysis> findByTransaction_Id(Long transactionId);
}

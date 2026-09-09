package com.bankapp.banking.controller;

import com.bankapp.banking.dto.FraudAnalysisResponse;
import com.bankapp.banking.service.FraudAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** ROLE_ADMIN only - see SecurityConfig for the matching path-level rule, enforced again here via @PreAuthorize. */
@RestController
@RequestMapping("/api/admin/fraud")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFraudController {

    private final FraudAdminService fraudAdminService;

    @GetMapping("/pending")
    public ResponseEntity<List<FraudAnalysisResponse>> getPendingReviews() {
        return ResponseEntity.ok(fraudAdminService.getPendingReviews());
    }

    @PostMapping("/{transactionId}/approve")
    public ResponseEntity<FraudAnalysisResponse> approve(@PathVariable Long transactionId) {
        return ResponseEntity.ok(fraudAdminService.approve(transactionId));
    }

    @PostMapping("/{transactionId}/reject")
    public ResponseEntity<FraudAnalysisResponse> reject(@PathVariable Long transactionId) {
        return ResponseEntity.ok(fraudAdminService.reject(transactionId));
    }
}

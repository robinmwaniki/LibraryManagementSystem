package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.FineAdjustment;
import com.example.librarymanagementsystem.model.FinePayment;
import com.example.librarymanagementsystem.service.FineLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Administrative endpoints for fine settlement and a tamper-resistant ledger history. */
@RestController @RequestMapping("/api/fines") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FineLedgerController {
    private final FineLedgerService fineLedgerService;
    public record PaymentRequest(BigDecimal amount, String reference) {}
    public record AdjustmentRequest(BigDecimal amount, String reason) {}

    @PostMapping("/{borrowingId}/payments")
    public ResponseEntity<FinePayment> pay(@PathVariable Long borrowingId,
            @RequestBody PaymentRequest request, Authentication authentication) {
        return ResponseEntity.ok(fineLedgerService.recordPayment(
                borrowingId, request.amount(), request.reference(), authentication.getName()));
    }
    @PostMapping("/{borrowingId}/adjustments")
    public ResponseEntity<FineAdjustment> adjust(@PathVariable Long borrowingId,
            @RequestBody AdjustmentRequest request, Authentication authentication) {
        return ResponseEntity.ok(fineLedgerService.adjustFine(
                borrowingId, request.amount(), request.reason(), authentication.getName()));
    }
    @GetMapping("/{borrowingId}/balance")
    public ResponseEntity<Map<String, BigDecimal>> balance(@PathVariable Long borrowingId) {
        return ResponseEntity.ok(Map.of("outstanding", fineLedgerService.outstandingForLoan(borrowingId)));
    }
    @GetMapping("/{borrowingId}/payments")
    public ResponseEntity<List<FinePayment>> payments(@PathVariable Long borrowingId) {
        return ResponseEntity.ok(fineLedgerService.paymentHistory(borrowingId));
    }
    @GetMapping("/{borrowingId}/adjustments")
    public ResponseEntity<List<FineAdjustment>> adjustments(@PathVariable Long borrowingId) {
        return ResponseEntity.ok(fineLedgerService.adjustmentHistory(borrowingId));
    }
    @GetMapping("/members/{memberId}/balance")
    public ResponseEntity<Map<String, BigDecimal>> memberBalance(@PathVariable Long memberId) {
        return ResponseEntity.ok(Map.of("outstanding", fineLedgerService.outstandingForMember(memberId)));
    }
}
package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.exception.ResourceNotFoundException;
import com.example.librarymanagementsystem.model.*;
import com.example.librarymanagementsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Records fine payments and signed adjustments without overwriting the original assessed fine. */
@Service @RequiredArgsConstructor @Transactional
public class FineLedgerService {
    private final BorrowingRepository borrowingRepository;
    private final FinePaymentRepository paymentRepository;
    private final FineAdjustmentRepository adjustmentRepository;
    private AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAuditService(AuditService auditService) { this.auditService = auditService; }

    public FinePayment recordPayment(Long borrowingId, BigDecimal amount, String reference, String actor) {
        BorrowingRecord loan = lockLoan(borrowingId);
        BigDecimal payment = money(amount);
        if (payment.signum() <= 0) throw new IllegalArgumentException("Payment amount must be greater than zero.");
        BigDecimal outstanding = outstandingFor(loan);
        if (payment.compareTo(outstanding) > 0)
            throw new IllegalArgumentException("Payment exceeds the outstanding fine of " + outstanding + ".");
        FinePayment receipt = new FinePayment();
        receipt.setBorrowing(loan); receipt.setAmount(payment); receipt.setRecordedBy(actor);
        receipt.setReference(reference == null || reference.isBlank() ? null : reference.trim());
        FinePayment saved = paymentRepository.save(receipt);
        if (auditService != null) auditService.record("FINE_PAYMENT", "FINE", borrowingId,
                "outstanding=" + outstanding, "outstanding=" + outstanding.subtract(payment));
        return saved;
    }

    public FineAdjustment adjustFine(Long borrowingId, BigDecimal amount, String reason, String actor) {
        BorrowingRecord loan = lockLoan(borrowingId);
        BigDecimal adjustment = money(amount);
        if (adjustment.signum() == 0) throw new IllegalArgumentException("Adjustment cannot be zero.");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("An adjustment reason is required.");
        BigDecimal revisedFine = assessed(loan).add(adjustmentRepository.sumByBorrowingId(borrowingId)).add(adjustment);
        BigDecimal paid = paymentRepository.sumByBorrowingId(borrowingId);
        if (revisedFine.signum() < 0 || revisedFine.compareTo(paid) < 0)
            throw new IllegalArgumentException("Adjustment cannot reduce the fine below zero or below the amount already paid.");
        BigDecimal outstandingBefore = revisedFine.subtract(paid).max(BigDecimal.ZERO);
        FineAdjustment entry = new FineAdjustment();
        entry.setBorrowing(loan); entry.setAmount(adjustment);
        entry.setReason(reason.trim()); entry.setAdjustedBy(actor);
        FineAdjustment saved = adjustmentRepository.save(entry);
        if (auditService != null) auditService.record("FINE_ADJUSTMENT", "FINE", borrowingId,
                "outstanding=" + outstandingBefore, "outstanding=" + outstandingBefore.add(adjustment));
        return saved;
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingForLoan(Long borrowingId) {
        BorrowingRecord loan = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing record not found"));
        return outstandingFor(loan);
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingForMember(Long memberId) {
        BigDecimal assessed = borrowingRepository.findByMemberId(memberId).stream()
                .map(this::assessed).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal adjustments = adjustmentRepository.sumByMemberId(memberId);
        BigDecimal payments = paymentRepository.sumByMemberId(memberId);
        return assessed.add(adjustments).subtract(payments).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<FinePayment> paymentHistory(Long borrowingId) {
        if (!borrowingRepository.existsById(borrowingId))
            throw new ResourceNotFoundException("Borrowing record not found");
        return paymentRepository.findByBorrowingIdOrderByPaidAtAscIdAsc(borrowingId);
    }

    @Transactional(readOnly = true)
    public List<FineAdjustment> adjustmentHistory(Long borrowingId) {
        if (!borrowingRepository.existsById(borrowingId))
            throw new ResourceNotFoundException("Borrowing record not found");
        return adjustmentRepository.findByBorrowingIdOrderByAdjustedAtAscIdAsc(borrowingId);
    }

    private BorrowingRecord lockLoan(Long id) {
        return borrowingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing record not found"));
    }
    private BigDecimal outstandingFor(BorrowingRecord loan) {
        return assessed(loan).add(adjustmentRepository.sumByBorrowingId(loan.getId()))
                .subtract(paymentRepository.sumByBorrowingId(loan.getId()))
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal assessed(BorrowingRecord loan) {
        return loan.getFine() == null ? BigDecimal.ZERO :
                BigDecimal.valueOf(loan.getFine()).setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal money(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Amount is required.");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
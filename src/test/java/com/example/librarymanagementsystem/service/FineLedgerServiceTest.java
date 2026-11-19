package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.FineAdjustment;
import com.example.librarymanagementsystem.model.FinePayment;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.FineAdjustmentRepository;
import com.example.librarymanagementsystem.repository.FinePaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineLedgerServiceTest {
    @Mock private BorrowingRepository borrowingRepository;
    @Mock private FinePaymentRepository paymentRepository;
    @Mock private FineAdjustmentRepository adjustmentRepository;
    @Mock private AuditService auditService;
    @InjectMocks private FineLedgerService fineLedgerService;

    @BeforeEach
    void injectAuditService() { fineLedgerService.setAuditService(auditService); }

    @Test
    void recordsPartialPaymentWithoutChangingAssessedFine() {
        BorrowingRecord loan = loan(12L, 100.0);
        when(borrowingRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(loan));
        when(paymentRepository.sumByBorrowingId(12L)).thenReturn(BigDecimal.ZERO);
        when(adjustmentRepository.sumByBorrowingId(12L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any(FinePayment.class))).thenAnswer(call -> call.getArgument(0));

        FinePayment payment = fineLedgerService.recordPayment(12L, new BigDecimal("35.50"), "RCPT-1", "admin");

        assertEquals(new BigDecimal("35.50"), payment.getAmount());
        assertEquals("admin", payment.getRecordedBy());
        assertEquals("RCPT-1", payment.getReference());
        assertEquals(100.0, loan.getFine());
        verify(auditService).record(eq("FINE_PAYMENT"), eq("FINE"), eq(12L), anyString(), anyString());
    }

    @Test
    void rejectsPaymentAboveOutstandingBalance() {
        when(borrowingRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(loan(12L, 100.0)));
        when(paymentRepository.sumByBorrowingId(12L)).thenReturn(new BigDecimal("80.00"));
        when(adjustmentRepository.sumByBorrowingId(12L)).thenReturn(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () ->
                fineLedgerService.recordPayment(12L, new BigDecimal("20.01"), null, "admin"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void requiresReasonAndPreventsAdjustmentBelowAmountAlreadyPaid() {
        when(borrowingRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(loan(12L, 100.0)));
        when(adjustmentRepository.sumByBorrowingId(12L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumByBorrowingId(12L)).thenReturn(new BigDecimal("80.00"));

        assertThrows(IllegalArgumentException.class, () ->
                fineLedgerService.adjustFine(12L, new BigDecimal("-21.00"), "Waiver", "admin"));
        assertThrows(IllegalArgumentException.class, () ->
                fineLedgerService.adjustFine(12L, new BigDecimal("-5.00"), " ", "admin"));
        verify(adjustmentRepository, never()).save(any(FineAdjustment.class));
    }

    private BorrowingRecord loan(Long id, double fine) {
        BorrowingRecord loan = new BorrowingRecord();
        loan.setId(id);
        loan.setFine(fine);
        return loan;
    }
}
package com.example.librarymanagementsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable-in-practice receipt for a payment against one assessed loan fine. */
@Entity
@Table(name = "fine_payments", indexes = @Index(name = "idx_fine_payments_borrowing", columnList = "borrowing_id,paid_at"))
@Getter @Setter @NoArgsConstructor
public class FinePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private BorrowingRecord borrowing;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;
    @Column(name = "recorded_by", nullable = false, length = 120)
    private String recordedBy;
    @Column(length = 500)
    private String reference;
    @PrePersist protected void onCreate() { if (paidAt == null) paidAt = LocalDateTime.now(); }
}
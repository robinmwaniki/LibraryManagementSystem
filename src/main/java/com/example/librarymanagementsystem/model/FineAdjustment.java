package com.example.librarymanagementsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Signed adjustment: positive adds a charge; negative records a waiver/reduction. */
@Entity
@Table(name = "fine_adjustments", indexes = @Index(name = "idx_fine_adjustments_borrowing", columnList = "borrowing_id,adjusted_at"))
@Getter @Setter @NoArgsConstructor
public class FineAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private BorrowingRecord borrowing;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "adjusted_at", nullable = false)
    private LocalDateTime adjustedAt;
    @Column(name = "adjusted_by", nullable = false, length = 120)
    private String adjustedBy;
    @PrePersist protected void onCreate() { if (adjustedAt == null) adjustedAt = LocalDateTime.now(); }
}
package com.example.librarymanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_reservations", indexes = {
        @Index(name = "idx_reservations_book_status_queue", columnList = "book_id,status,created_at,id"),
        @Index(name = "idx_reservations_member_status", columnList = "member_id,status"),
        @Index(name = "idx_reservations_expiry", columnList = "status,expires_at")
})
@Data
@NoArgsConstructor
public class BookReservation {

    public enum ReservationStatus {
        WAITING, READY, COLLECTED, CANCELLED, EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.WAITING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = ReservationStatus.WAITING;
    }
}

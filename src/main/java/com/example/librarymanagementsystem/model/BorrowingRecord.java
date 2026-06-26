package com.example.librarymanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "borrowing_records")
@Data
@NoArgsConstructor
public class BorrowingRecord {

    public enum BorrowStatus {
        BORROWED, RETURNED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "borrow_date", nullable = false)
    private LocalDateTime borrowDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BorrowStatus status = BorrowStatus.BORROWED;

    private Double fine;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BorrowStatus.BORROWED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

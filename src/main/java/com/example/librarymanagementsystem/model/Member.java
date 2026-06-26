package com.example.librarymanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
public class Member {

    public enum MembershipStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "max_books_allowed")
    private Integer maxBooksAllowed = 5;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MembershipStatus.ACTIVE;
        }
        if (this.maxBooksAllowed == null) {
            this.maxBooksAllowed = 5;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

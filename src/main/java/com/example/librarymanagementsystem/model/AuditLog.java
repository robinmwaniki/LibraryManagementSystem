package com.example.librarymanagementsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Append-only operational audit entry. Summary fields deliberately exclude member contact data and credentials. */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_actor_action", columnList = "actor, action"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, updatable = false)
    private String actor;

    @Column(nullable = false, length = 80, updatable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 80, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100, updatable = false)
    private String entityId;

    @Column(name = "before_summary", length = 1000, updatable = false)
    private String beforeSummary;

    @Column(name = "after_summary", length = 1000, updatable = false)
    private String afterSummary;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void setOccurredAt() {
        if (occurredAt == null) occurredAt = LocalDateTime.now();
    }
}

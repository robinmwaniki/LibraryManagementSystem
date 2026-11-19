package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/** Intentionally exposes no update/delete operations for audit records. */
public interface AuditLogRepository extends Repository<AuditLog, Long> {
    <S extends AuditLog> S save(S entry);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:startInclusive IS NULL OR a.occurredAt >= :startInclusive)
          AND (:endExclusive IS NULL OR a.occurredAt < :endExclusive)
          AND (:actor IS NULL OR :actor = '' OR LOWER(a.actor) LIKE LOWER(CONCAT('%', :actor, '%')))
          AND (:action IS NULL OR :action = '' OR a.action = :action)
          AND (:entityType IS NULL OR :entityType = '' OR a.entityType = :entityType)
          AND (:entityId IS NULL OR :entityId = '' OR a.entityId = :entityId)
        ORDER BY a.occurredAt DESC, a.id DESC
        """)
    Page<AuditLog> search(
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("actor") String actor,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            Pageable pageable);
}

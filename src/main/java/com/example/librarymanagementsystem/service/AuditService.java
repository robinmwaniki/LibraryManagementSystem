package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.AuditLog;
import com.example.librarymanagementsystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/** Writes append-only audit events in the same transaction as the business operation. */
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|token|secret|authorization|cookie)\\s*[:=]\\s*[^,;\\s]+");
    private final AuditLogRepository repository;

    public AuditLog record(String action, String entityType, Object entityId,
                           String beforeSummary, String afterSummary) {
        AuditLog entry = new AuditLog();
        entry.setActor(currentActor());
        entry.setAction(required(action, 80));
        entry.setEntityType(required(entityType, 80));
        entry.setEntityId(required(String.valueOf(entityId), 100));
        entry.setBeforeSummary(safeSummary(beforeSummary));
        entry.setAfterSummary(safeSummary(afterSummary));
        return repository.save(entry);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "system";
        }
        return required(authentication.getName(), 120);
    }

    private String safeSummary(String value) {
        if (value == null || value.isBlank()) return null;
        String safe = SECRET_PATTERN.matcher(value).replaceAll("$1=[REDACTED]")
                .replaceAll("[\\r\\n\\t]", " ");
        return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
    }

    private String required(String value, int maxLength) {
        if (value == null || value.isBlank()) return "unknown";
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}

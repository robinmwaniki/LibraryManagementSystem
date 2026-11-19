package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.AuditLog;
import com.example.librarymanagementsystem.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAuthenticatedActorAndRedactsCredentialLikeValues() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(any(AuditLog.class))).thenAnswer(call -> call.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("library-admin", "not-for-audit", "ROLE_ADMIN"));
        AuditService service = new AuditService(repository);

        AuditLog saved = service.record("UPDATE", "BOOK", 15L,
                "title=Old,token=old-secret", "title=New,password=top-secret");

        assertEquals("library-admin", saved.getActor());
        assertEquals("UPDATE", saved.getAction());
        assertEquals("BOOK", saved.getEntityType());
        assertEquals("15", saved.getEntityId());
        assertEquals("title=Old,token=[REDACTED]", saved.getBeforeSummary());
        assertEquals("title=New,password=[REDACTED]", saved.getAfterSummary());
        verify(repository).save(saved);
    }

    @Test
    void usesSystemActorWhenNoAuthenticatedPrincipalExists() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(any(AuditLog.class))).thenAnswer(call -> call.getArgument(0));

        AuditLog saved = new AuditService(repository).record("BORROW", "BORROWING", 8L, null, "status=BORROWED");

        assertEquals("system", saved.getActor());
        assertNull(saved.getBeforeSummary());
    }
}
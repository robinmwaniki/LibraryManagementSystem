package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.AuditLog;
import com.example.librarymanagementsystem.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditControllerTest {
    @Test
    void searchUsesInclusiveEndDateAndAllProvidedFilters() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        Page<AuditLog> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(repository.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);
        AuditController controller = new AuditController(repository);

        controller.history(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                "admin", "UPDATE", "BOOK", "12", 0, 20, new ExtendedModelMap());

        verify(repository).search(
                eq(LocalDate.of(2026, 11, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 12, 1).atStartOfDay()),
                eq("admin"), eq("UPDATE"), eq("BOOK"), eq("12"), any());
    }

    @Test
    void invalidDateRangeReturnsEmptyPageWithoutQuery() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditController controller = new AuditController(repository);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.history(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1),
                "", "", "", "", -2, 500, model);

        verifyNoInteractions(repository);
        assertEquals(true, model.get("invalidRange"));
        assertEquals(100, model.get("pageSize"));
        assertEquals(0, ((Page<?>) model.get("entries")).getTotalElements());
    }
}
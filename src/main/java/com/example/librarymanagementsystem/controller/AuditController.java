package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Administrator-only audit history search screen. */
@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public String history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "") String actor,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String entityType,
            @RequestParam(defaultValue = "") String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        boolean invalidRange = startDate != null && endDate != null && startDate.isAfter(endDate);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 5), 100);
        var entries = invalidRange ? org.springframework.data.domain.Page.empty(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt")))
                : auditLogRepository.search(
                    startDate == null ? null : startDate.atStartOfDay(),
                    endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                    normalize(actor), normalize(action), normalize(entityType), normalize(entityId),
                    PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt")));
        model.addAttribute("entries", entries);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("actor", actor);
        model.addAttribute("action", action);
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityId", entityId);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("invalidRange", invalidRange);
        model.addAttribute("actions", java.util.List.of("CREATE", "UPDATE", "DELETE", "STATUS_CHANGE",
                "BORROW", "RETURN", "RENEW", "FINE_PAYMENT", "FINE_ADJUSTMENT"));
        model.addAttribute("entityTypes", java.util.List.of("BOOK", "MEMBER", "BORROWING", "FINE"));
        return "audit";
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

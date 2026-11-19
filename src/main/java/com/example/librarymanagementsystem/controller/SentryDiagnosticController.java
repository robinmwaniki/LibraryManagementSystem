package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.observability.SentryDiagnosticService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Optional, administrator-only diagnostic endpoint. It is absent unless explicitly enabled
 * and should be disabled again after an operator verifies Sentry delivery.
 */
@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "library.observability", name = "test-endpoint-enabled", havingValue = "true")
public class SentryDiagnosticController {
    private final SentryDiagnosticService diagnosticService;

    @PostMapping("/sentry-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> sendSentryTestEvent() {
        String eventId = diagnosticService.captureControlledTestEvent();
        return ResponseEntity.accepted().body(Map.of("status", "submitted", "eventId", eventId));
    }
}

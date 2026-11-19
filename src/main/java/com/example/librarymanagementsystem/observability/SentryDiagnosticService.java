package com.example.librarymanagementsystem.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Sends a deliberately generic test event only when an operator has configured Sentry. */
@Service
@RequiredArgsConstructor
public class SentryDiagnosticService {
    private final SentryEventCapture eventCapture;
    @Value("${sentry.dsn:}")
    private String dsn;

    public String captureControlledTestEvent() {
        if (!StringUtils.hasText(dsn)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Sentry is not configured; set SENTRY_DSN before testing event delivery.");
        }
        return eventCapture.capture(new IllegalStateException("Controlled Sentry integration verification event"));
    }
}

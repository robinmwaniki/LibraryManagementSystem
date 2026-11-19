package com.example.librarymanagementsystem.observability;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SentryDiagnosticServiceTest {
    @Test
    void reportsUnavailableWhenDsnIsNotConfigured() {
        SentryEventCapture capture = mock(SentryEventCapture.class);
        SentryDiagnosticService service = new SentryDiagnosticService(capture);
        ReflectionTestUtils.setField(service, "dsn", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                service::captureControlledTestEvent);

        assertEquals(503, error.getStatusCode().value());
        verifyNoInteractions(capture);
    }

    @Test
    void capturesGenericControlledExceptionWhenDsnIsConfigured() {
        SentryEventCapture capture = mock(SentryEventCapture.class);
        when(capture.capture(any(Throwable.class))).thenReturn("event-123");
        SentryDiagnosticService service = new SentryDiagnosticService(capture);
        ReflectionTestUtils.setField(service, "dsn", "https://public@example.ingest.sentry.io/123");

        assertEquals("event-123", service.captureControlledTestEvent());
        verify(capture).capture(argThat(error ->
                error instanceof IllegalStateException
                        && error.getMessage().equals("Controlled Sentry integration verification event")));
    }
}

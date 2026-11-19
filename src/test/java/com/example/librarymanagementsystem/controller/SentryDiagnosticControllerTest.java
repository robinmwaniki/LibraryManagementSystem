package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.config.SecurityConfig;
import com.example.librarymanagementsystem.observability.SentryDiagnosticService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SentryDiagnosticController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "library.observability.test-endpoint-enabled=true")
class SentryDiagnosticControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private SentryDiagnosticService diagnosticService;

    @Test
    void administratorCanSubmitControlledSentryEvent() throws Exception {
        when(diagnosticService.captureControlledTestEvent()).thenReturn("event-123");

        mockMvc.perform(post("/api/diagnostics/sentry-test")
                        .with(user("library-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("submitted"))
                .andExpect(jsonPath("$.eventId").value("event-123"));

        verify(diagnosticService).captureControlledTestEvent();
    }

    @Test
    void nonAdministratorCannotSubmitSentryEvent() throws Exception {
        mockMvc.perform(post("/api/diagnostics/sentry-test")
                        .with(user("reader").roles("MEMBER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(diagnosticService);
    }
}

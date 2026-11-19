package com.example.librarymanagementsystem.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void acceptsSafeCallerCorrelationIdAndReturnsItInResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-123.safe");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("request-123.safe", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNull(MDC.get("correlationId"), "MDC must be cleared after request completion");
    }

    @Test
    void replacesInvalidCallerValueWithGeneratedUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "bad id\r\nAuthorization: secret");
        filter.doFilter(request, response, new MockFilterChain());

        assertDoesNotThrow(() -> UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER_NAME)));
        assertNull(MDC.get("correlationId"));
    }
}

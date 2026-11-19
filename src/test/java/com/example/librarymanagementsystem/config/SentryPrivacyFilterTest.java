package com.example.librarymanagementsystem.config;

import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SentryPrivacyFilterTest {
    @Test
    void removesRequestAndUserContextAndRedactsFreeFormMessages() {
        SentryEvent event = new SentryEvent();
        Request request = new Request();
        request.setUrl("https://library.example/members?email=member@example.test");
        event.setRequest(request);
        User user = new User();
        user.setEmail("member@example.test");
        event.setUser(user);
        event.setBreadcrumbs(List.of(new io.sentry.Breadcrumb()));
        Message message = new Message();
        message.setMessage("Failed login for member@example.test password=top-secret");
        event.setMessage(message);
        SentryException exception = new SentryException();
        exception.setValue("Unable to process member@example.test sessionId=abc123");
        event.setExceptions(List.of(exception));

        SentryEvent sanitized = new SentryPrivacyFilter().execute(event, null);

        assertNull(sanitized.getRequest());
        assertNull(sanitized.getUser());
        assertTrue(sanitized.getBreadcrumbs().isEmpty());
        assertEquals("[event message redacted]", sanitized.getMessage().getMessage());
        assertEquals("[exception message redacted]", sanitized.getExceptions().get(0).getValue());
    }

    @Test
    void dropsNullEventsSafely() {
        assertNull(new SentryPrivacyFilter().execute(null, null));
    }
}

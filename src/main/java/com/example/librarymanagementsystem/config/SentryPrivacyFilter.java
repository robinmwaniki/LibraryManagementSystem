package com.example.librarymanagementsystem.config;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.SentryException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Applies a privacy-first policy before an event leaves the process.
 * Request/user context, breadcrumbs and free-form exception messages are removed;
 * exception type and stack frames remain available for diagnosis.
 */
@Component
public class SentryPrivacyFilter implements SentryOptions.BeforeSendCallback {
    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        if (event == null) {
            return null;
        }

        event.setRequest(null);
        event.setUser(null);
        event.setBreadcrumbs(List.of());

        if (event.getMessage() != null) {
            event.getMessage().setMessage("[event message redacted]");
        }
        if (event.getExceptions() != null) {
            for (SentryException exception : event.getExceptions()) {
                exception.setValue("[exception message redacted]");
            }
        }
        return event;
    }
}

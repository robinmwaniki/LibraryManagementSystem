package com.example.librarymanagementsystem.observability;

import io.sentry.Sentry;
import org.springframework.stereotype.Component;

/** Small seam around the SDK so diagnostic capture can be tested without network access. */
@Component
public class SentryEventCapture {
    public String capture(Throwable throwable) {
        return Sentry.captureException(throwable).toString();
    }
}

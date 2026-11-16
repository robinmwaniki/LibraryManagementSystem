# Library Management System — Code Review

Reviewed branch: master
Reviewed commit: f41a79ab1c5f28b73a116e4f7dcdc4599ff46606

## Current stack
- Spring Boot 3.2.0, Java compiler configured for 21 while java.version says 17.
- Spring MVC, Thymeleaf, Bootstrap 5.3.2, Spring Data JPA, MySQL.
- Separate REST and server-rendered controllers.
- Existing Books, Members, Borrowings and Settings modules.

## Findings

### Critical
1. Settings access uses a custom session Boolean and plaintext manager password (library.manager.password=admin123); there is no application-wide authentication or authorization.
2. The repository has no CI workflow and only a context-load test.
3. The application configuration embeds local database defaults and enables SQL logging / schema mutation by default.

### High
1. Borrowing reads a book and member without pessimistic locking. Concurrent requests can oversubscribe inventory or a member's loan limit.
2. Book updates can reset available copies to total copies, incorrectly making borrowed copies available.
3. Web delete endpoints use GET, which is unsafe and vulnerable to accidental triggering.
4. The dashboard and list pages load full collections into memory rather than using aggregate queries and pagination.
5. Return operations can be submitted concurrently; fine calculation and inventory restoration need idempotent locking.
6. Book/member delete operations do not explicitly protect records referenced by borrowing history.

### Medium
1. Categories are free-text strings, allowing inconsistent values.
2. No reporting module, notification service, SMTP configuration, or scheduled overdue reminders exists.
3. Existing Java version declarations conflict (17 vs 21).
4. Global exception handling is REST-oriented and returns JSON for server-rendered requests.
5. main.js references jQuery without declaring a jQuery dependency, and its path comment does not match its actual location.
6. The current test suite does not cover business rules, security, persistence, or MVC behavior.

## Upgrade approach
Preserve MySQL, Thymeleaf, Bootstrap, existing route families and existing data. Implement the upgrade in small reviewable phases:
1. Authentication and secure configuration.
2. Inventory-safe borrowing and returns.
3. Pageable search, categories and inventory controls.
4. Dashboard analytics and reports.
5. Configurable mail reminders.
6. Automated tests and CI.

## Verification status
This is a static source review of the GitHub default branch. No local Maven build, live database, SMTP server, or browser test was available during this review. The upgrade branch must pass CI and be exercised against a backed-up local database before production use.

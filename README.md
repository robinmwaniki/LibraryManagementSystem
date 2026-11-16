# Library Management System

A Spring Boot and Thymeleaf application for managing a library catalogue, members, borrowing, returns and overdue loans.

## Technology
- Java 21
- Spring Boot 3.2
- Spring MVC, Thymeleaf and Bootstrap 5
- Spring Data JPA and MySQL
- Spring Security (BCrypt password hashing)
- Spring Mail (optional overdue reminders)
- Maven

## Local setup

1. Install JDK 21, Maven (or use the included Maven wrapper), and MySQL. XAMPP is supported.
2. Create a MySQL database named library_db, or allow the configured JDBC URL to create it.
3. Set the administrator and database environment variables before starting the application.

### Windows PowerShell

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/library_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD=""
$env:LIBRARY_ADMIN_USERNAME="admin"
$env:LIBRARY_ADMIN_PASSWORD="replace-with-a-long-unique-password"
.mvnw.cmd spring-boot:run
```

Open http://localhost:8080 and sign in using the administrator credentials configured above. Do not commit real passwords or SMTP credentials.

## Optional overdue email reminders

Set these environment variables to enable SMTP delivery:

- MAIL_HOST
- MAIL_PORT (default 587)
- MAIL_USERNAME
- MAIL_PASSWORD
- MAIL_FROM (optional sender address)
- LIBRARY_NOTIFICATIONS_ENABLED=true
- LIBRARY_REMINDER_CRON (default: every day at 08:00 server time)

Reminders are rate-limited per borrowing record to at most one every 24 hours. SMTP must be configured and tested before enabling notifications.

## Tests

Run the test suite:

```powershell
.mvnw.cmd --batch-mode clean verify
```

The test profile uses an in-memory H2 database and does not connect to the local MySQL instance.

## Existing data and upgrades

The application currently retains Hibernate schema update mode to avoid breaking existing installations while the database baseline is reviewed. Back up library_db before upgrading. A versioned Flyway migration plan is the next database-hardening step; do not change to validate-only until the existing schema has been reconciled with the migration baseline.

## Security notes

- Configure a strong administrator password through environment variables.
- The application requires authentication for management pages and API routes.
- Spring Security CSRF protection is enabled. Browser forms should submit CSRF tokens; API clients must obtain and submit a valid token when using state-changing endpoints.
- Use HTTPS and a managed secrets store for any network-accessible deployment.

# Testing Guide

This project uses a layered test strategy so business rules, web security, database migrations, and the Docker deployment can be checked independently.

## Requirements

- JDK 21
- Maven (or the included Maven Wrapper)
- Docker Desktop / Docker Engine for the PostgreSQL Testcontainers test and Docker Compose smoke test

## Run the complete Maven verification

On Windows PowerShell:

```powershell
.mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

On macOS or Linux:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

The test profile uses an isolated H2 database for Spring application-context and web-security tests. The PostgreSQL/Liquibase integration test uses Testcontainers and starts a disposable PostgreSQL 18 container. If Docker is unavailable, Testcontainers marks that integration test as skipped; run the command again with Docker running before treating PostgreSQL migration verification as complete.

## Test coverage

| Test class | Responsibility |
| --- | --- |
| `BookServiceTest` | Book update rules, inventory consistency, search normalization and pagination |
| `MemberServiceTest` | Member registration, duplicate email protection, and deletion safeguards |
| `BorrowingServiceTest` | Borrowing eligibility, stock changes, returns, overdue fine caps, renewals, and reservation queue hand-off |\n| `ReservationServiceTest` | Duplicate prevention, FIFO allocation, cancellation, expiry promotion, and reservation collection |
| `SecurityIntegrationTest` | Anonymous access restrictions, administrator authentication, and public health endpoint |
| `PaginationControllerTest` | REST pagination bounds and allow-listed catalogue sort properties |
| `PostgreSqlLiquibaseIntegrationTest` | PostgreSQL 18 schema creation, Liquibase changesets, Hibernate mapping validation, real database pagination and catalogue search |
| `LibraryManagementSystemApplicationTests` | Spring application context startup |

## Catalogue search, filtering and sorting

The book catalogue search is case-insensitive and matches partial terms across title, author, ISBN, publisher, and category. Search can be combined with an exact category filter and an availability-only filter. Repository queries apply these criteria before pagination, so the database returns only the requested page.

The HTML catalogue and REST collection endpoint support these sort keys:

| Sort key | Database field | Default direction |
| --- | --- | --- |
| `title` | `title` | Ascending |
| `author` | `author` | Ascending |
| `publicationDate` | `publicationYear` | Ascending |
| `recentlyAdded` | `createdAt` | Descending |

Unknown sort keys fall back to recently added. The REST endpoint also accepts `direction=asc` or `direction=desc`; recently added is always newest first. Sort keys are allow-listed to prevent arbitrary property paths from being supplied by clients.

Example:

```http
GET /api/books?search=spring&category=Technology&availableOnly=true&sort=title&direction=asc&page=0&size=10
```

The HTML list preserves search, category, availability, sort, direction, and page-size criteria while navigating between pages. Empty results display a prompt to adjust or clear the filters.

## Pagination regression coverage

Book and member service tests verify that queries pass a `Pageable` to Spring Data repositories, preserve active search and filter criteria, report total elements and pages, and return safe empty results for out-of-range page indexes. `PaginationControllerTest` verifies that REST endpoints clamp negative page indexes, cap page sizes at 100, and reject unknown catalogue sort keys by falling back to a safe default. The PostgreSQL integration test verifies database-level pagination and catalogue query behavior.

## Paginated list endpoints

The REST list endpoints return Spring Data page metadata alongside the current page content:

- `GET /api/books?page=0&size=10&search=spring&category=Technology&availableOnly=true&sort=title&direction=asc`
- `GET /api/members?page=0&size=20&search=robin&status=ACTIVE`

Both endpoints default to page `0` and size `10`. Negative page indexes are normalized to page zero, and requested page sizes are bounded to a maximum of `100`. The response includes `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, and `last`.

## Book reservations and waiting lists

A member can reserve a title with no available copies and enter its FIFO waiting list. A title with available stock can be reserved immediately; that copy is held by reducing the available inventory count. When a loan is returned, the oldest waiting reservation becomes `READY` and its copy is held. Staff can inspect the reservation register at `/reservations` or use the reservation REST endpoints.

A ready reservation expires after the configurable number of days (default: 3). The expiry scheduler runs hourly by default, releases the unclaimed copy, and promotes the next waiting member. Collection and cancellation requests also process expired holds for that title, so the queue does not depend solely on the scheduled job.

Configuration:

- `LIBRARY_RESERVATION_EXPIRY_DAYS`: ready-hold period in days (1–30; default 3).
- `LIBRARY_RESERVATION_EXPIRY_CRON`: six-field Spring cron expression for the expiry job (default: hourly).

API examples:

- `POST /api/reservations?bookId=1&memberId=2`: create a reservation or join the queue.
- `GET /api/reservations/member/2`: list a member's reservation statuses.
- `GET /api/reservations/book/1/queue`: inspect a title's FIFO queue.
- `POST /api/reservations/12/collect`: record collection and create a borrowing record.
- `DELETE /api/reservations/12?memberId=2`: cancel a reservation.

The title's database row is locked before queue changes. This serializes duplicate checks, queue promotion, cancellation, expiry, and collection for the same book. Reservation queue order uses request timestamp followed by the generated ID as a deterministic tie-breaker. A member may have only one active (`WAITING` or `READY`) reservation for a title.

The PostgreSQL Testcontainers test verifies the Liquibase reservation table, persisted queue ordering, and expired-ready lookup. Unit tests cover cancellation and promotion, expiry and promotion, and collection into a loan.

## Docker Compose smoke test

The CI workflow also builds and starts the full application stack after Maven verification. It waits for `/actuator/health` and prints Compose logs if startup fails.

To reproduce the deployment smoke test locally:

1. Copy `.env.example` to `.env` and set unique local credentials.
2. Start Docker Desktop / Docker Engine.
3. Run:

   ```bash
   docker compose up --build --detach
   ```

4. Confirm `http://localhost:8080/actuator/health` reports healthy.
5. Inspect startup failures with `docker compose logs`.
6. Stop the stack with `docker compose down`.

Do not commit the local `.env` file or real credentials.


## Fine assessment, payments and adjustments

The daily fine rate and optional maximum fine are configured from the authenticated **Settings** page. The rate is applied to calendar days after the due date; returning on the due date is not late. The original assessed fine remains on the borrowing record. Payments and signed adjustments are stored as separate ledger rows with timestamps and the authenticated administrator's username.

- A payment must be positive and cannot exceed the outstanding balance.
- A fine adjustment must be non-zero and include a reason. Positive values add a charge; negative values record a waiver/reduction.
- An adjustment cannot reduce the fine below zero or below amounts already paid.
- Payment and adjustment history is retained; no ledger entries are overwritten or deleted by the application.
- Member directory rows display the outstanding fine balance.

Administrative API examples (authenticated administrator):

```http
POST /api/fines/42/payments
Content-Type: application/json

{"amount": 50.00, "reference": "RECEIPT-2026-0042"}
```

```http
POST /api/fines/42/adjustments
Content-Type: application/json

{"amount": -10.00, "reason": "Approved partial waiver"}
```

Read the current balance and ledger history with `GET /api/fines/42/balance`, `GET /api/fines/42/payments`, and `GET /api/fines/42/adjustments`. A member's aggregate outstanding balance is available at `GET /api/fines/members/7/balance`. Fine ledger endpoints require the ADMIN role.


## Reporting filters and CSV exports

The reports page supports optional inclusive start and end dates, exact book category, and borrowing status (`BORROWED` or `RETURNED`). The end date is converted to midnight at the start of the following day and used as an exclusive upper bound, so all records on the selected end date are included. A start date later than the end date displays a validation error and returns no report rows. CSV export uses the same filters and date semantics.

Filtered reporting includes borrowing activity, overdue counts within the result set, recorded fines, the most-borrowed titles, and the most active members. The existing unfiltered overdue-loans CSV remains available at `/reports/overdue.csv`.

CSV text fields are quoted and embedded quotes are doubled. Text beginning with spreadsheet formula initiators (`=`, `+`, `-`, `@`, tab, carriage return, or line feed) is prefixed with an apostrophe to reduce CSV formula-injection risk. `ReportControllerTest` covers inclusive end-date handling, invalid ranges, export filtering, and formula-injection mitigation.

Example requests:

```http
GET /reports?startDate=2026-11-01&endDate=2026-11-30&category=Technology&status=RETURNED
GET /reports/export.csv?startDate=2026-11-01&endDate=2026-11-30&category=Technology&status=RETURNED
```


## Audit history

Important catalogue, member, circulation, and fine-ledger operations write an audit entry in the same database transaction as the business change. Entries include the authenticated actor (or `system` for unauthenticated scheduled/system work), action, record type and ID, timestamp, and concise before/after summaries. Member summaries intentionally omit names, email addresses, phone numbers, and postal addresses; fine summaries omit payment references and adjustment reasons. Credential-like values are redacted before persistence.

Audit history is available to administrators at `/audit`. Filters include inclusive date range, actor username, action, record type, and record ID. Results are newest first and paginated. The audit repository exposes only insert and search operations; the application does not provide audit edit or delete actions. The Liquibase migration creates indexes for time, actor/action, and record lookups.

`AuditServiceTest` verifies actor attribution and secret redaction. `AuditControllerTest` verifies search filters and inclusive end-date handling. Service tests verify that book updates, member registration, borrowing, and fine payments emit audit events. `SecurityIntegrationTest` verifies that the audit screen redirects anonymous users to sign-in and is available to the configured administrator. The PostgreSQL integration test verifies the audit table and migration count.


## PostgreSQL backup and restore verification

The Docker Compose database service is named postgres. The backup script runs pg_dump in custom archive format against the live database; it does not stop the application. It writes to a temporary file with restrictive permissions, validates the archive with pg_restore --list, and only then publishes the final dump.

```bash
bash scripts/backup-postgres.sh
bash scripts/backup-postgres.sh backups/before-release.dump
bash scripts/restore-postgres.sh backups/before-release.dump library_restore_check
bash scripts/restore-postgres.sh backups/before-release.dump
```

The restore script requires an existing backup file, validates the archive, validates the target database identifier, creates the target database when absent, and restores with --clean --if-exists --single-transaction --exit-on-error. By default the target is the configured application database; the script requires the operator to type RESTORE before proceeding. Use a separate target database first when validating a recovery. Restoring to the configured live database replaces objects from the archive and should only be done after writes have been stopped or the application has been put into maintenance mode.

The CI smoke test inserts a known book, member, and borrowing record, takes a backup while the database remains online, restores into a separate clean database, and verifies all three record types. The smoke test runs after the Docker Compose health check.

### Recovery checklist

1. Identify the required recovery point and select a verified backup from protected storage.
2. Confirm the target PostgreSQL version and available disk space.
3. Keep the application in maintenance mode or stop application writes.
4. Restore into a clean, isolated database first and validate books, members, borrowing records, fine ledger, audit history, and Liquibase metadata.
5. Update the application database connection only after verification; restart and check /actuator/health.
6. Validate sign-in, catalogue search, member records, circulation, and reports before reopening traffic.
7. Retain the pre-restore database snapshot until recovery is accepted.

Backup files contain library and member records and must be treated as sensitive data. Store them outside the repository, restrict access, encrypt them at rest and during transfer, and maintain an off-host/off-site copy. Do not include credentials or .env files in backups or commit dump files. The Docker named volume postgres_data is persistent storage, not a backup: deleting the volume or losing the Docker host can destroy the data.


## Sentry, structured logging, and telemetry privacy

The application uses the Jakarta Spring Boot Sentry starter and Logstash Logback JSON encoder. Sentry is disabled when SENTRY_DSN is blank; local development, tests, and deployments do not require an external Sentry account. Configure DSN, environment, release, and sample rates through environment variables only.

Privacy controls:
- Sentry default PII collection is disabled and request-body capture is set to none.
- The before-send callback removes request and user context and all breadcrumbs.
- Free-form event and exception messages are redacted; exception class and stack frames remain for diagnosis.
- Do not place credentials, session IDs, member names/contact details, borrowing notes, payment references, or fine adjustment reasons in log messages, tags, extras, or custom contexts.
- Do not enable Sentry log forwarding or performance tracing by default. Trace sample rate defaults to zero.
- Keep DSNs and deployment secrets in environment settings or a secrets manager; do not commit them.

Every HTTP request has an X-Correlation-ID. A caller-supplied value is accepted only when it matches the restricted 1–64 character allow-list; otherwise a UUID is generated. The value is returned in the response, placed in MDC during request processing, and emitted in structured JSON logs. MDC is cleared after the request to prevent thread-pool leakage.

Actuator exposes health, info, metrics, and Prometheus metrics. Only /actuator/health is public; details are hidden unless the caller is authenticated. The remaining actuator endpoints require authentication. Database health details are never shown to anonymous callers.

The controlled Sentry test endpoint is disabled unless LIBRARY_OBSERVABILITY_TEST_ENDPOINT_ENABLED=true. When temporarily enabled, only an administrator can POST to /api/diagnostics/sentry-test. The endpoint emits a generic test exception and returns the Sentry event ID. Disable the flag again after confirming delivery. It is not intended as a permanent public endpoint.

Tests cover correlation ID validation and cleanup, Sentry event scrubbing, DSN-absent behavior, diagnostic event capture delegation, and administrator-only diagnostic access. The application-context test also verifies that Sentry is optional when no DSN is configured.

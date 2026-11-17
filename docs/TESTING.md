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

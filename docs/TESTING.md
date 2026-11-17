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
| `BookServiceTest` | Book update rules and inventory consistency |
| `MemberServiceTest` | Member registration, duplicate email protection, and deletion safeguards |
| `BorrowingServiceTest` | Borrowing eligibility, stock changes, returns, overdue fine caps, and renewals |
| `SecurityIntegrationTest` | Anonymous access restrictions, administrator authentication, and public health endpoint |
| `PostgreSqlLiquibaseIntegrationTest` | PostgreSQL 18 schema creation, Liquibase changesets, and Hibernate mapping validation |
| `LibraryManagementSystemApplicationTests` | Spring application context startup |

## Pagination regression coverage

Book and member service tests verify that queries pass a `Pageable` to Spring Data repositories, preserve active search and filter criteria, report total elements and pages, and return safe empty results for out-of-range page indexes. `PaginationControllerTest` verifies that REST endpoints clamp negative page indexes and cap page sizes at 100. The REST list endpoints and HTML list pages use database-backed pagination; list page sizes are bounded to prevent unbounded reads.

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

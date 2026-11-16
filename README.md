# Library Management System

A Spring Boot + Thymeleaf library application for catalogue management, members, borrowing, returns, overdue tracking and reporting.

## Technology

- Java 21 and Spring Boot 3.2
- Spring MVC, Thymeleaf and Bootstrap 5
- Spring Data JPA with **PostgreSQL 18**
- **Liquibase** versioned database migrations
- Spring Security with BCrypt password hashing
- Optional Spring Mail overdue reminders
- Docker and Docker Compose
- Maven

## Run with Docker (recommended)

1. Install Docker Desktop and start it.
2. Copy `.env.example` to `.env`.
3. Replace `LIBRARY_ADMIN_PASSWORD` and `DB_PASSWORD` with strong unique values.
4. Start the application:

```bash
docker compose up --build
```

The application is available at http://localhost:8080. PostgreSQL is exposed on localhost:5432 for local database tools. The database data is stored in the named Docker volume `postgres_data`.

To stop the services:

```bash
docker compose down
```

To also delete the local database volume and start with a completely fresh database (destructive):

```bash
docker compose down -v
docker compose up --build
```

## Deploy to Render (development/staging)

Render runs the application as a Docker Web Service and the database as a separate Render PostgreSQL instance. Do not deploy the local `docker-compose.yml` stack as a single Render service.

1. Create a PostgreSQL database in Render. Keep it in the same region as the web service so the application can use the database's **internal host**.
2. Create a Web Service connected to this repository and select the `master` branch.
3. Set the runtime to **Docker** and use the repository-root `Dockerfile`.
4. Set the health check path to `/actuator/health`.
5. Add these environment variables to the web service:
   - `DB_URL`: `jdbc:postgresql://<INTERNAL_HOST>:5432/<DATABASE_NAME>?sslmode=require`
   - `DB_USERNAME`: the Render database username
   - `DB_PASSWORD`: the Render database password
   - `LIBRARY_ADMIN_USERNAME`: the initial administrator username
   - `LIBRARY_ADMIN_PASSWORD`: a strong, unique administrator password
   - `LIBRARY_NOTIFICATIONS_ENABLED`: `false` until SMTP is configured and tested
6. Deploy and inspect the service logs. Liquibase should apply the initial changesets before the application becomes ready.

Render supplies the `PORT` environment variable to web services. The application uses it automatically, while retaining `SERVER_PORT`/8080 as the local and Docker Compose fallback. The Spring Boot server binds to all interfaces by default.

Use the Render PostgreSQL **internal** host, not the public connection host, when the web service and database are in the same region. Keep database and administrator credentials in Render environment settings; never commit them to Git. Render web-service filesystems are ephemeral, so store persistent application data in a managed database or an appropriate persistent storage service rather than the container filesystem.

For this staging deployment, use a separate Render database from any future production database. Verify login, catalogue and member management, borrowing/returns, Liquibase startup, health checks, and data persistence before planning the AWS deployment.

## Run locally without Docker

Requirements: JDK 21, Maven, and PostgreSQL 18.

Create a database and user (or use your own credentials):

```sql
CREATE USER library WITH PASSWORD 'change-this-password';
CREATE DATABASE library_db OWNER library;
```

Set environment variables before starting the application. On Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/library_db"
$env:DB_USERNAME="library"
$env:DB_PASSWORD="change-this-password"
$env:LIBRARY_ADMIN_USERNAME="admin"
$env:LIBRARY_ADMIN_PASSWORD="replace-with-a-long-unique-password"
mvn spring-boot:run
```

Liquibase creates and versions the schema on startup. Hibernate runs in `validate` mode; do not use `ddl-auto=update` alongside Liquibase.

## Database migrations

Changelogs are stored in `src/main/resources/db/changelog`. Add a new, uniquely identified changeset for every schema change and never edit a changeset that has already been applied to a shared database.

The initial changelog creates the application's tables and indexes for a fresh PostgreSQL database. Liquibase records applied changes in `databasechangelog` and `databasechangeloglock`.

## Optional overdue email reminders

Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, and set `LIBRARY_NOTIFICATIONS_ENABLED=true`. Reminders are rate-limited per borrowing record to at most one every 24 hours. Verify SMTP delivery before enabling this feature.

## Tests

```bash
mvn --batch-mode clean verify
```

The test profile uses an isolated in-memory H2 database and disables Liquibase; production and Docker use PostgreSQL with Liquibase.

## Security

- Set strong, unique administrator and database passwords in `.env`; never commit secrets.
- Authentication is required for management pages and API routes.
- Spring Security CSRF protection remains enabled.
- Use HTTPS and a secrets manager for network-accessible deployments.

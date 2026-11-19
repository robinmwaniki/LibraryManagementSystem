#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BACKUP_FILE="${1:-}"
TARGET_DB="${2:-}"
CONFIRM="${3:-}"

if [[ -z "$BACKUP_FILE" || ! -f "$BACKUP_FILE" ]]; then
  echo "Usage: bash scripts/restore-postgres.sh <backup-file> [target-database] [--yes]" >&2
  exit 2
fi

if ! docker compose ps --status running --services | grep -qx postgres; then
  echo "The Docker Compose postgres service must be running." >&2
  exit 1
fi

PG_USER="$(docker compose exec -T postgres printenv POSTGRES_USER | tr -d '\r')"
SOURCE_DB="$(docker compose exec -T postgres printenv POSTGRES_DB | tr -d '\r')"
TARGET_DB="${TARGET_DB:-$SOURCE_DB}"

# Restrict identifiers before using them in SQL or as psql/createdb arguments.
if [[ ! "$TARGET_DB" =~ ^[A-Za-z_][A-Za-z0-9_]{0,62}$ ]]; then
  echo "Invalid target database name. Use 1-63 letters, digits or underscores; it must start with a letter or underscore." >&2
  exit 2
fi

echo "Validating backup archive..."
docker compose exec -T postgres pg_restore --list < "$BACKUP_FILE" >/dev/null

if [[ "$CONFIRM" != "--yes" ]]; then
  echo "WARNING: restoring into '$TARGET_DB' will replace objects contained in this backup."
  read -r -p "Type RESTORE to continue: " answer
  if [[ "$answer" != "RESTORE" ]]; then
    echo "Restore cancelled."
    exit 1
  fi
fi

EXISTS="$(docker compose exec -T postgres psql -U "$PG_USER" -d "$SOURCE_DB" -tAc \
  "SELECT 1 FROM pg_database WHERE datname = '$TARGET_DB'" | tr -d '[:space:]')"
if [[ "$EXISTS" != "1" ]]; then
  echo "Creating clean target database: $TARGET_DB"
  docker compose exec -T postgres createdb -U "$PG_USER" "$TARGET_DB"
fi

echo "Restoring $BACKUP_FILE into database $TARGET_DB..."
docker compose exec -T postgres pg_restore \
  --clean --if-exists --single-transaction --exit-on-error \
  --no-owner --no-acl -U "$PG_USER" -d "$TARGET_DB" < "$BACKUP_FILE"

echo "Restore completed. Verify application data before directing traffic to this database."

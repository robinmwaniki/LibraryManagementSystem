#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

umask 077
BACKUP_FILE="${1:-backups/library-$(date -u +%Y%m%dT%H%M%SZ).dump}"
mkdir -p "$(dirname "$BACKUP_FILE")"
TEMP_FILE="${BACKUP_FILE}.partial"
trap 'rm -f "$TEMP_FILE"' EXIT

if ! docker compose ps --status running --services | grep -qx postgres; then
  echo "The Docker Compose postgres service must be running." >&2
  exit 1
fi

echo "Creating PostgreSQL custom-format backup: $BACKUP_FILE"
docker compose exec -T postgres sh -c \
  'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner --no-acl' \
  > "$TEMP_FILE"

if [[ ! -s "$TEMP_FILE" ]]; then
  echo "Backup output is empty; refusing to publish an invalid backup." >&2
  exit 1
fi

# Validate the archive using the pg_restore binary in the same PostgreSQL container.
docker compose exec -T postgres pg_restore --list < "$TEMP_FILE" >/dev/null
mv "$TEMP_FILE" "$BACKUP_FILE"
trap - EXIT
chmod 600 "$BACKUP_FILE"
echo "Backup completed and archive validation passed: $BACKUP_FILE"

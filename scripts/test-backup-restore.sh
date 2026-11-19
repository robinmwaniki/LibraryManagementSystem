#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
BACKUP_FILE="$TEMP_DIR/library-backup-smoke.dump"
TARGET_DB="library_restore_smoke"

PG_USER="$(docker compose exec -T postgres printenv POSTGRES_USER | tr -d '\r')"
SOURCE_DB="$(docker compose exec -T postgres printenv POSTGRES_DB | tr -d '\r')"

# Insert a deterministic catalogue/member/loan fixture before taking the live backup.
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$SOURCE_DB" <<'SQL'
INSERT INTO books (title, author, isbn, total_copies, available_copies, category)
VALUES ('Backup Restore Smoke Book', 'CI Verification', '9780000099988', 1, 0, 'Verification')
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO members (first_name, last_name, email, phone, status, max_books_allowed)
VALUES ('Backup', 'Restore', 'backup.restore.smoke@example.test', '+254700000099', 'ACTIVE', 5)
ON CONFLICT (email) DO NOTHING;

INSERT INTO borrowing_records (book_id, member_id, borrow_date, due_date, status, fine, renewal_count)
SELECT b.id, m.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '14 days', 'BORROWED', 0, 0
FROM books b CROSS JOIN members m
WHERE b.isbn = '9780000099988'
  AND m.email = 'backup.restore.smoke@example.test'
  AND NOT EXISTS (
      SELECT 1 FROM borrowing_records existing
      WHERE existing.book_id = b.id AND existing.member_id = m.id
        AND existing.status = 'BORROWED'
  );
SQL

bash scripts/backup-postgres.sh "$BACKUP_FILE"
bash scripts/restore-postgres.sh "$BACKUP_FILE" "$TARGET_DB" --yes

BOOK_COUNT="$(docker compose exec -T postgres psql -U "$PG_USER" -d "$TARGET_DB" -tAc \
  "SELECT COUNT(*) FROM books WHERE isbn = '9780000099988'" | tr -d '[:space:]')"
MEMBER_COUNT="$(docker compose exec -T postgres psql -U "$PG_USER" -d "$TARGET_DB" -tAc \
  "SELECT COUNT(*) FROM members WHERE email = 'backup.restore.smoke@example.test'" | tr -d '[:space:]')"
LOAN_COUNT="$(docker compose exec -T postgres psql -U "$PG_USER" -d "$TARGET_DB" -tAc \
  "SELECT COUNT(*) FROM borrowing_records br JOIN books b ON b.id = br.book_id JOIN members m ON m.id = br.member_id WHERE b.isbn = '9780000099988' AND m.email = 'backup.restore.smoke@example.test'" | tr -d '[:space:]')"

[[ "$BOOK_COUNT" == "1" && "$MEMBER_COUNT" == "1" && "$LOAN_COUNT" -ge "1" ]] || {
  echo "Backup/restore verification failed: books=$BOOK_COUNT members=$MEMBER_COUNT loans=$LOAN_COUNT" >&2
  exit 1
}
echo "Backup/restore smoke test passed: book=$BOOK_COUNT member=$MEMBER_COUNT loan-records=$LOAN_COUNT"

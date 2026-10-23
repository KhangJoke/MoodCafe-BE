#!/usr/bin/env bash
set -euo pipefail
cd /opt/moodcafe
source ./runtime.conf
# Fresh instance has no application deployed yet.
[ -n "$BACKUP_BUCKET" ] && [ -f .env ] && [ -f .image ] || exit 0
export BACKEND_IMAGE
BACKEND_IMAGE=$(cat .image)
# Prevent overlapping manual and scheduled backups.
exec 9>/run/moodcafe-backup.lock
flock -n 9 || exit 0
umask 077
BACKUP_FILE=$(mktemp /opt/moodcafe/postgres-backup.XXXXXX)
trap 'rm -f -- "$BACKUP_FILE"' EXIT
docker compose --env-file .env -f compose.yml exec -T postgres \
  sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$BACKUP_FILE"
[ -s "$BACKUP_FILE" ]
aws s3 cp "$BACKUP_FILE" "s3://$BACKUP_BUCKET/postgres/$(date -u +%Y-%m-%dT%H-%M-%SZ).dump" \
  --region "$AWS_REGION" --sse AES256 --only-show-errors
echo 'PostgreSQL backup uploaded to S3'


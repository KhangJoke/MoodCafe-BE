#!/usr/bin/env bash
set -euo pipefail
cd /opt/moodcafe
source ./runtime.conf
[ -f .env ] || { echo 'Create /opt/moodcafe/.env from .env.example first'; exit 1; }
if grep -q 'CHANGE_ME' .env; then
  echo 'Replace the CHANGE_ME placeholders in .env before deploying'
  exit 1
fi
chmod 600 .env

if [ "$#" -gt 0 ]; then
  TAG=$1
  [[ "$TAG" =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]] || { echo 'Invalid image tag'; exit 1; }
  BACKEND_IMAGE="$ECR_REPOSITORY:$TAG"
elif [ -f .image ]; then
  BACKEND_IMAGE=$(cat .image)
else
  echo 'Usage: sudo /opt/moodcafe/deploy.sh <published-ecr-tag>'
  exit 1
fi
export BACKEND_IMAGE
REGISTRY=$(printf '%s' "$ECR_REPOSITORY" | cut -d/ -f1)
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY"
trap 'docker logout "$REGISTRY" >/dev/null 2>&1 || true' EXIT
docker compose --env-file .env -f compose.yml config --quiet
docker compose --env-file .env -f compose.yml pull
docker compose --env-file .env -f compose.yml up -d

# Wait for the BE public configuration endpoint, not merely a running container.
for attempt in $(seq 1 60); do
  if docker compose --env-file .env -f compose.yml exec -T proxy \
      wget -q -O /dev/null http://backend:8080/api/configs/public; then
    printf '%s\n' "$BACKEND_IMAGE" > .image
    chmod 600 .image
    echo "Backend ready: $BACKEND_IMAGE"
    exit 0
  fi
  sleep 5
done
echo 'BE did not become ready within 5 minutes. Check container logs; data has been preserved.'
exit 1


#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."

docker compose stop worker
printf '%s\n' \
    'Worker stopped. Save and submit a declaration at http://localhost:13000.' \
    'It stays submitted until you run: docker compose start worker' \
    'After restart, inspect the final decision and the three audit entries.' \
    'For broker recovery, stop kafka, submit, then start kafka. The outbox retries.'

#!/bin/bash
# SPDX-License-Identifier: MPL-2.0

set -euo pipefail

app_home=${1:-/app}
archive="$app_home/startup.jsa"
training_log=$(mktemp)
backend_pid=
cleanup() {
    if [[ -n "$backend_pid" ]]; then
        kill -TERM "$backend_pid" 2>/dev/null || true
        wait "$backend_pid" 2>/dev/null || true
    fi
    rm -f "$training_log"
}
trap cleanup EXIT

env -u JAVA_TOOL_OPTIONS -u BACKEND_OPTS \
    PORT=8080 IGDB_RATE_LIMITER=in-memory REDIS_URL= PUBLIC_BASE_URL= \
    TWITCH_CLIENT_ID=archive-training TWITCH_CLIENT_SECRET=archive-training \
    JAVA_OPTS="-XX:MaxRAM=512m -XX:MaxRAMPercentage=50.0 -XX:ArchiveClassesAtExit=$archive" \
    "$app_home/bin/backend" >"$training_log" 2>&1 &
backend_pid=$!

ready=false
for ((attempt = 0; attempt < 600; attempt++)); do
    if grep -q '"phase":"server_ready"' "$training_log"; then
        ready=true
        break
    fi
    if ! kill -0 "$backend_pid" 2>/dev/null; then
        cat "$training_log"
        exit 1
    fi
    sleep 0.05
done
if [[ "$ready" != true ]]; then
    cat "$training_log"
    echo 'Archive training did not reach server readiness' >&2
    exit 1
fi

exec 3<>/dev/tcp/127.0.0.1/8080
printf 'GET /health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3
response=
while IFS= read -r -t 5 line <&3 || [[ -n "$line" ]]; do
    response+="$line"
done
exec 3<&-
exec 3>&-
if [[ "$response" != HTTP/1.1\ 200* || "$response" != *'{"ok":true}'* ]]; then
    echo 'Archive training health check failed' >&2
    exit 1
fi

kill -TERM "$backend_pid"
status=0
wait "$backend_pid" || status=$?
backend_pid=
if [[ "$status" != 0 && "$status" != 143 ]]; then
    cat "$training_log"
    exit "$status"
fi
if [[ ! -s "$archive" ]]; then
    cat "$training_log"
    echo 'JVM did not produce the startup archive' >&2
    exit 1
fi

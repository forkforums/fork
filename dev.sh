#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-5000}"
SEEDS="${2:-${FORK_SEEDS:-}}"
HOST="${FORK_HOST:-0.0.0.0}"
BASE_URL="${FORK_BASE_URL:-http://localhost:${PORT}}"

export FORK_PORT="$PORT"
export FORK_HOST="$HOST"
export FORK_BASE_URL="$BASE_URL"

if [[ -n "$SEEDS" ]]; then
  export FORK_SEEDS="$SEEDS"
fi

if [[ -n "${FORK_SEEDS:-}" ]]; then
  echo "Using seeds: ${FORK_SEEDS}"
fi

exec lein run

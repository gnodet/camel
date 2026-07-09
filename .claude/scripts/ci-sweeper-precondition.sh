#!/usr/bin/env bash
# CI Sweeper loop precondition script.
#
# Exit 0  → a failure exists on a watched branch; run the sweeper.
# Exit 1  → all branches green or in-progress; skip this iteration.
#
# This script runs BEFORE the model is invoked, so it costs zero tokens
# when CI is green.  The full sweeper skill is only loaded when there is
# an actual failure to triage.

set -euo pipefail

REPO="apache/camel"

# ── Kill switch ──────────────────────────────────────────────────────
STATE_FILE="$(git rev-parse --show-toplevel)/STATE.md"
if [[ -f "$STATE_FILE" ]]; then
  if grep -qP '^loop-pause-all' "$STATE_FILE" 2>/dev/null; then
    echo "precondition: kill switch active — skipping"
    exit 1
  fi
fi

# ── Check main branch ───────────────────────────────────────────────
main_conclusion=$(gh run list --repo "$REPO" --branch main \
  --workflow "Main build" --limit 1 \
  --json conclusion --jq '.[0].conclusion' 2>/dev/null || echo "unknown")

if [[ "$main_conclusion" == "failure" ]]; then
  echo "precondition: FAILURE on main — proceeding"
  exit 0
fi

# ── Check camel-4.18.x branch ───────────────────────────────────────
branch_conclusion=$(gh api "repos/${REPO}/actions/runs?branch=camel-4.18.x&per_page=1&status=completed" \
  --jq '.workflow_runs[0].conclusion // "none"' 2>/dev/null || echo "unknown")

if [[ "$branch_conclusion" == "failure" ]]; then
  echo "precondition: FAILURE on camel-4.18.x — proceeding"
  exit 0
fi

# ── All green ────────────────────────────────────────────────────────
echo "precondition: all watched branches green (main=$main_conclusion, 4.18.x=$branch_conclusion) — skipping"
exit 1

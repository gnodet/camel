---
name: loop-budget
description: >
  Check token budget and run-log spend before and after a loop run.
  Enforces early exit when over budget or when CI is green.
---

# Loop Budget Guard

Run at the **start** and **end** of every loop iteration.

## Start of Run

1. Read `loop-budget.md` for daily caps and kill-switch flags.
2. Read recent entries in `loop-run-log.md` (last 24h).
3. Sum `tokens_estimate` for the `ci-sweeper` pattern today.
4. If spend >= 80% of daily cap → **report-only mode** (triage but no fix agents).
5. If spend >= 100% or `loop-pause-all` is set → **exit immediately** with a one-line
   note in STATE.md.
6. If CI is green on all watched branches → **exit in < 5k tokens** (do not spawn
   sub-agents for a green build).

## End of Run

Append one JSON object to `loop-run-log.md`:

```json
{
  "run_id": "<ISO8601>",
  "pattern": "ci-sweeper",
  "duration_s": <number>,
  "branch": "<branch>",
  "failures_found": <number>,
  "fixes_proposed": <number>,
  "escalations": <number>,
  "tokens_estimate": <number>,
  "outcome": "no-op | report-only | fix-proposed | escalated"
}
```

## Rules

- Never exceed `max sub-agent spawns/run` from `loop-budget.md`.
- CI Sweeper **must** early-exit when CI is green — do not run the full pipeline.
- On self-throttle, append a warning line to the Escalated section in STATE.md.

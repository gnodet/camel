# Loop Run Log — Apache Camel CI Sweeper

Append one entry per run. Prune entries older than 30 days.

## Format

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

## Recent Runs

<!-- Loop appends below this line -->

```json
{
  "run_id": "2026-07-09T15:00:00Z",
  "pattern": "ci-sweeper",
  "duration_s": 15,
  "branch": "main, camel-4.18.x",
  "failures_found": 0,
  "fixes_proposed": 0,
  "escalations": 0,
  "tokens_estimate": 5000,
  "outcome": "no-op"
}
```

```json
{
  "run_id": "2026-07-09T15:45:00Z",
  "pattern": "ci-sweeper",
  "duration_s": 10,
  "branch": "main, camel-4.18.x",
  "failures_found": 0,
  "fixes_proposed": 0,
  "escalations": 0,
  "tokens_estimate": 5000,
  "outcome": "no-op"
}
```

```json
{
  "run_id": "2026-07-09T17:15:00Z",
  "pattern": "ci-sweeper",
  "duration_s": 8,
  "branch": "main, camel-4.18.x",
  "failures_found": 0,
  "fixes_proposed": 0,
  "escalations": 0,
  "tokens_estimate": 5000,
  "outcome": "no-op"
}
```

```json
{
  "run_id": "2026-07-09T19:15:00Z",
  "pattern": "ci-sweeper",
  "duration_s": 6,
  "branch": "main",
  "failures_found": 0,
  "fixes_proposed": 0,
  "escalations": 0,
  "tokens_estimate": 3000,
  "outcome": "no-op"
}
```

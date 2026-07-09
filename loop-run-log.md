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

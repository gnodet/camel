# Loop Budget — Apache Camel PR Review Loop

## Daily limits

| Loop | Max runs/day | Max tokens/day | Max sub-agent spawns/run |
|------|--------------|----------------|--------------------------|
| pr-review-loop | 48 | 10M | 10 |

## On budget exceed

1. Switch to report-only mode
2. Append event to `loop-run-log.md`
3. Log warning in STATE.md Escalated section

## Kill switch

- Set `loop-pause-all` flag in STATE.md to halt all loop activity
- Resume only after human clears the flag

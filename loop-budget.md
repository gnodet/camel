# Loop Budget — Apache Camel CI Sweeper

> Primary loop: **CI Sweeper** (fix-capable)

## Daily limits

| Loop | Max runs/day | Max tokens/day | Max sub-agent spawns/run |
|------|--------------|----------------|--------------------------|
| CI Sweeper | 96 (every 15m) | 10M | 4 (2 implementers + 2 verifiers) |

## On budget exceed

1. Switch to report-only mode (triage without spawning fix agents)
2. Append event to `loop-run-log.md`
3. Log warning in STATE.md Escalated section

## Kill switch

- Set `loop-pause-all` flag in STATE.md to halt all loop activity
- Resume only after human clears the flag

## Cost profile

| Scenario | Tokens/run | Notes |
|----------|------------|-------|
| No-op (CI green) | ~5k | Early exit — don't run the full sweeper when green |
| Triage/classify | ~50k | Log parsing and failure classification |
| Fix attempt | ~200k | Full worktree + implementer + verifier cycle |

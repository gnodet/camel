# Loop Configuration — Camel CI Sweeper

| Pattern | Cadence | Status |
|---------|---------|--------|
| CI Sweeper (fix-capable) | 15m (active) / 30m (overnight) | L2 assisted |

## Limits

- Max fix attempts per failure: 3 (then escalate)
- Max failures acted on per run: 2
- Max files changed per fix: 5 (larger scope = escalate)
- Auto-merge: **disabled** (always draft PR)
| Watched branches | main, camel-4.18.x |
| CI workflow | Main build |

## Human Gates

- Failures touching > 5 files
- Failures in security-sensitive paths (auth, secrets, SSL/TLS)
- Infrastructure failures (runner OOM, registry down, missing secrets)
- Flaky tests needing quarantine (not a code fix)
- Same failure recurring after 3 fix attempts

## Safety

- Never merge, close, or label PRs
- Never push to branches you didn't create
- Always propose fixes as draft PRs on the operator's fork
- Verifier sub-agent must confirm fix before PR is opened
- Kill switch: set `loop-pause-all` in STATE.md to halt

## Budget

- See `loop-budget.md` for token caps
- If spend hits 80% of daily cap, switch to report-only (triage without fixing)
- Circuit breaker: `loop-ledger.json` tracks attempts per failure, max 3

## Architecture

```
Main loop (orchestrator) → discover + classify inline
  ├── Implementer sub-agent per failure (worktree isolation, via Agent tool)
  ├── Verifier sub-agent per fix (parallel, maker/checker)
  └── Open draft PR for verified fixes, update STATE.md
```

## Skills Used

| Skill | Role |
|-------|------|
| `/forgebot-ci-sweeper` | Main orchestrator — discover, classify, dispatch, state |
| `/forgebot-ci-triage` | Classify each failure (flake, regression, infra) |
| `/forgebot-minimal-fix` | Craft smallest possible fix for a specific failure |
| `loop-verifier` (agent) | Independent checker — confirms fix is correct |
| `/forgebot-loop-guard` | Circuit breaker — checks attempt limits |
| `/forgebot-loop-budget` | Token spend tracking |
| `/forgebot-loop-constraints` | Binding rules (denylist paths, merge policy) |

## Invocation

```
cd /path/to/camel-ci-sweeper && claude
/loop 15m /forgebot-ci-sweeper
```

Or for a single run:
```
/forgebot-ci-sweeper
```

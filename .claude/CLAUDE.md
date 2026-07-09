# Camel CI Sweeper Loop

This checkout is dedicated to running an automated CI sweeper loop for Apache Camel.
It monitors CI failures on `main` (and optionally release branches), classifies them,
and proposes minimal fixes via PRs on the operator's fork.

## Purpose

Periodically check CI status on watched branches of `apache/camel`. When failures are
detected, triage them (flake vs regression vs infra), and for actionable regressions,
propose the smallest possible fix as a draft PR.

## Loop Configuration

- **Pattern:** CI Sweeper (fix-capable)
- **Cadence:** Run via `/loop` — recommended: `/loop 15m /ci-sweeper`
- **Level:** L2 — fixes are proposed as draft PRs, never merged
- **Watched branches:** `main`, `camel-4.18.x` (active maintenance)
- **Max fix attempts per failure:** 3 (circuit breaker via loop-guard)

## Architecture

The loop uses a **triage-then-fix sub-agent pattern**:

1. **Main loop** (orchestrator) — discovers failures, classifies inline
2. **Implementer sub-agents** — one per fixable failure, spawned in worktrees.
   Each uses the `minimal-fix` skill to produce the smallest diff.
3. **Verifier sub-agents** — one per fix, spawned after implementers complete.
   Each independently confirms the fix addresses root cause, passes tests,
   and introduces no unrelated changes.

## Safety Rules

- NEVER push to any branch you did not create
- NEVER merge, close, or label PRs
- NEVER force-push to any branch
- ALWAYS create fixes on the operator's fork (`gnodet/camel`), not `apache/camel`
- ALWAYS open PRs as **draft** — human decides when to undraft
- ALWAYS run the verifier sub-agent before opening a PR
- ALWAYS include AI attribution in PRs and commits
- ALWAYS update STATE.md after each run
- ALWAYS check git history before proposing fixes
- ALWAYS run `loop-guard` before each fix attempt

## Attribution

All PRs and commits must include:
"_This fix was proposed by an AI agent and should be reviewed before merging._"

And include:
"_Claude Code on behalf of Guillaume Nodet_"

## Maven

When running Maven commands, always add `-B` (batch mode).
For module-specific builds: `mvn clean install -B -pl <module> -am`

## State Tracking

`STATE.md` in the repo root tracks:
- Last run timestamp
- Active CI failures with classification and attempt count
- Proposed fixes (PRs) and their status
- Escalated failures requiring human attention
- Resolved failures (rolling 7-day window)

## Kill Switch

Set `loop-pause-all` in STATE.md to halt all loop activity.

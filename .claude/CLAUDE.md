# Camel CI Sweeper Loop

This checkout is dedicated to running an automated CI sweeper loop for Apache Camel.
It monitors CI failures on `main` (and optionally release branches), classifies them,
and proposes minimal fixes via PRs on the operator's fork.

## Skills

This loop uses **forgebot-skills** (installed from
`https://gitea.gnodet.fr/gnodet/forgebot-skills`). All skills are globally installed
under `~/.claude/skills/forgebot-*` — there are no local skill overrides in this
checkout.

See `LOOP.md` for the full skill inventory and loop configuration.

## Quick Start

```bash
# Single run
/forgebot-ci-sweeper

# Continuous loop with precondition (recommended)
/loop 15m --precondition .claude/scripts/ci-sweeper-precondition.sh /forgebot-ci-sweeper
```

### Precondition Script

The loop uses a two-tier precondition (`.claude/scripts/ci-sweeper-precondition.sh`)
to minimize API cost:

1. **Tier 1 — ETag check**: Conditional request to GitHub Events API. A `304 Not
   Modified` response means no repo activity — exits at **zero API cost**.
2. **Tier 2 — CI check**: Only runs when ETag changes. Checks each watched branch
   for a failed CI run (~1 API call per branch).

The script reads watched branches and CI workflow name from `LOOP.md`.

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

## State Tracking

`STATE.md` in the repo root tracks:
- Last run timestamp
- Active CI failures with classification and attempt count
- Proposed fixes (PRs) and their status
- Escalated failures requiring human attention
- Resolved failures (rolling 7-day window)

## Kill Switch

Set `loop-pause-all` in STATE.md to halt all loop activity.

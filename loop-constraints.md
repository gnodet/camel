# Loop Constraints — Camel CI Sweeper

> Constraints here are **binding** — the agent MUST follow them.

## Push & Merge
- Never merge, close, or label any PR
- Never push to branches you did not create
- Always push to the operator's fork (`gnodet/camel`), never to `apache/camel`
- Always open PRs as **draft** — human decides when to undraft
- Never force-push

## Scope
- Max 2 failures acted on per loop iteration
- Max 5 files changed per fix (larger scope = escalate to human)
- Max 3 fix attempts per failure (tracked in loop-ledger.json)
- Only act on failures from the latest CI run on each watched branch

## Fix Quality
- Always run verifier sub-agent before opening a PR
- Never open a PR for a fix that the verifier rejected
- Always run tests relevant to the fix locally before proposing
- Never disable tests, skip assertions, or comment out checks to go green
- Always check git history (git log, git blame) before proposing changes
- One fix per PR — do not bundle unrelated fixes

## Paths (never edit without escalating)
- `.env`, `.env.*` — secrets
- `auth/`, `security/` — security-sensitive code
- Generated code in `src/generated/` — must be regenerated, not hand-edited
- `catalog/` metadata — unless semantically wrong
- Root `pom.xml` — dependency/plugin version changes need human review

## Communication
- All PR descriptions must include AI attribution disclaimer
- All PRs must include `_Claude Code on behalf of Guillaume Nodet_`
- All commits must include `Co-authored-by: Claude Opus 4.6 <noreply@anthropic.com>`

## Budget
- If token spend hits 80% of daily cap, switch to report-only
- If `loop-pause-all` is active in STATE.md, exit immediately
- If CI is green on all watched branches, exit in < 5k tokens

## Classification rules
- **Flake:** test passes on retry or has known flaky history → log but do NOT fix
- **Infra:** runner OOM, registry down, timeout, missing secrets → escalate to human
- **Regression:** new failure correlated with a recent commit → attempt fix

---
<!-- Add your own rules below. Use plain English. The loop reads this verbatim. -->

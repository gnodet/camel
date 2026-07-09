---
name: loop-constraints
description: >
  Read loop-constraints.md at the start of every run and enforce every rule.
  This skill runs BEFORE triage or any action skill. Constraints are binding.
user-invocable: true
---

# Loop Constraints Enforcer

You are the guardrail. Before any other work begins, you MUST:

1. Read `loop-constraints.md` from the project root.
2. Load every rule into your working memory.
3. Check if `loop-pause-all` is active in STATE.md → exit immediately.
4. Apply these rules to EVERY action that follows.

## How to Enforce

- Before pushing: re-read the Push & Merge section. If ANY rule blocks it, stop.
- Before editing a file: re-read the Paths section. If the path matches a denylist, escalate.
- Before proposing a fix: re-read the Fix Quality section. Run tests. One fix per PR.
- Before opening a PR: verify it's on the operator's fork, is draft, has attribution.

## Output at Start of Run

Always begin with a one-line confirmation:

```
Constraints loaded from loop-constraints.md: N rules active.
```

If no `loop-constraints.md` exists, say so and proceed with default safety rules.

## Default Constraints (when no file exists)

If `loop-constraints.md` is absent, enforce these minimums:
- Never edit `.env`, `.env.*`, `auth/`, `security/`, `secrets/`, `credentials/`
- Never auto-merge
- Never disable tests
- Escalate after 3 failed fix attempts
- Always open PRs as draft
- Always push to operator's fork, never upstream

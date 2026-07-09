---
name: loop-guard
description: >
  Circuit breaker for the CI sweeper loop. Before each fix attempt, check
  loop-ledger.json; if the same failure has been attempted too many times
  or token budget is exceeded, escalate to a human instead of retrying.
user-invocable: true
---

# Loop Guard (Circuit Breaker)

Prevents the CI sweeper from burning tokens on a problem it cannot solve.
Wraps every fix attempt with a deterministic circuit-breaker check.

## The Ledger

`loop-ledger.json` records the loop's goal and one entry per attempt:

```json
{
  "goal": "Keep watched branches CI green",
  "pattern": "ci-sweeper",
  "level": "L2",
  "attempts": [
    {
      "iteration": 1,
      "failure": "TestFoo#testBar in camel-kafka",
      "action": "patch null check in KafkaConsumer",
      "outcome": "failure",
      "error": "AssertionError: expected 200 got 500",
      "tokensUsed": 180000,
      "timestamp": "2026-07-09T10:00:00Z"
    }
  ]
}
```

`outcome` is `success | failure | noop`. Always include `error` on failures.

## Before Each Fix Attempt

1. Read `loop-ledger.json`.
2. Count attempts for the **same failure** (match by job name + error signature).
3. Check against thresholds:
   - **Stagnation:** same error message 3x in a row → ESCALATE
   - **No progress:** 5 consecutive failures (any failure) → ESCALATE
   - **Iteration cap:** 10 total attempts across all failures → ESCALATE
   - **Token budget:** sum of `tokensUsed` exceeds daily cap → ESCALATE
4. Return the decision:

```
LOOP_GUARD: CONTINUE | ESCALATE

If ESCALATE:
  REASON: <stagnation | no-progress | iteration-cap | token-budget>
  DETAILS: <specific counts and thresholds>
  SUGGESTION: <what the human should look at>
```

## After Each Fix Attempt

Append the attempt to `loop-ledger.json`:

```json
{
  "iteration": <N>,
  "failure": "<job/test identifier>",
  "action": "<what was tried>",
  "outcome": "<success|failure|noop>",
  "error": "<error message if failure>",
  "tokensUsed": <estimated tokens>,
  "timestamp": "<ISO8601>"
}
```

## On Escalate

1. Write the escalation into STATE.md "Escalated (human required)" section:
   ```
   - **<failure>**: <N> attempts failed. Last error: <error>.
     Recommendation: <suggestion for human>.
   ```
2. Exit the fix loop for this failure. Do NOT retry.
3. Continue processing other failures if within limits.

## Rules

- Never widen thresholds just to keep looping — escalation is a feature.
- Never edit the ledger to hide a repeated error.
- A verifier rejection counts as a `failure` — log it.
- A flake classification counts as `noop` — log it but don't count toward failure caps.
- Defaults: 3x same error, 5 consecutive failures, 10 iterations total.

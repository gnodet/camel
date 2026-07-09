---
name: ci-sweeper
description: >
  CI sweeper loop for Apache Camel. Monitors CI on watched branches, classifies
  failures (flake, regression, infra), and proposes minimal fixes as draft PRs.
  Uses sub-agents for parallel fix and verification with a maker/checker pattern.
  Tracks state in STATE.md.
user-invocable: true
---

# CI Sweeper Loop

Automated loop that discovers CI failures on watched branches, classifies them,
and proposes minimal fixes. Uses worktree-isolated sub-agents for fixes and
independent verifiers before opening draft PRs.

## Architecture

```
Main loop (orchestrator)
  ├── Step 1-3: Discover & classify (inline — fast, low cost)
  │
  ├── Step 4: Fix (sub-agents, parallel, worktree-isolated)
  │     ├── Implementer agent failure #1 (worktree)
  │     └── Implementer agent failure #2 (worktree)
  │
  ├── Step 5: Verify (sub-agents, one per fix)
  │     ├── Verifier agent fix #1  ← checks implementer's work
  │     └── Verifier agent fix #2
  │
  └── Step 6-8: Open draft PRs, update state, push (inline)
```

## Execution Steps

### 0. Pre-flight Checks

1. Read `loop-constraints.md` — load all binding rules.
2. Check for `loop-pause-all` in STATE.md — if set, exit immediately.
3. Read `loop-budget.md` and `loop-run-log.md` — check daily spend.
   If spend >= 80%, switch to report-only mode (triage but no fixes).
   If spend >= 100%, exit immediately.
4. Read `loop-ledger.json` — check circuit breaker state.

### 1. Discover CI Failures

For each watched branch (`main`, `camel-4.18.x`), get the latest CI run:

```bash
# Get the latest workflow run on the branch
gh run list --repo apache/camel --branch main \
  --workflow "Main build" --limit 1 \
  --json databaseId,status,conclusion,headSha,createdAt \
  --jq '.[0]'
```

If the latest run is `completed` with `conclusion: success` → CI is green on this
branch. Log it and move on to the next branch.

If the latest run is `completed` with `conclusion: failure`:

```bash
# Get failed jobs
gh run view <RUN_ID> --repo apache/camel \
  --json jobs --jq '.jobs[] | select(.conclusion == "failure") | {name, conclusion, steps: [.steps[] | select(.conclusion == "failure") | .name]}'
```

```bash
# Download the log for the failed run
gh run view <RUN_ID> --repo apache/camel --log-failed 2>/dev/null | tail -500
```

If the run is still `in_progress` or `queued`, skip this branch — don't act on
incomplete runs.

### 2. Check Against State

Read `STATE.md` and compare each failure against:

- **Active Failures** table — is this the same failure (same job name + similar error)?
  If yes, increment the attempt count. If attempts >= 3, escalate instead of retrying.
- **Resolved** table — was this failure recently resolved but came back? Flag as
  regression of a regression.
- **Proposed Fixes** table — is there already a draft PR for this failure? If yes,
  check the PR status instead of re-attempting.

### 3. Classify Failures

For each new or updated failure, classify it:

**Flake detection:**
```bash
# Check if this test has failed before and then passed (flaky pattern)
gh run list --repo apache/camel --branch main --workflow "Main build" \
  --limit 10 --json conclusion --jq '[.[].conclusion]'
```
If the same job alternates between success and failure → likely flake.
Also check if the error message matches known flaky patterns:
- Timeout / connection refused in integration tests
- Port already in use
- Intermittent assertion failures with timing sensitivity

**Infrastructure detection:**
- Runner OOM / killed / disk full
- Docker registry rate limit / timeout
- Maven repository unreachable
- GitHub Actions service degradation

**Regression detection:**
- New failure not seen in prior runs
- Error correlates with recent commit(s)
- Failure is deterministic (same error in recent runs)

For regressions, identify the likely culprit commit:
```bash
# Find commits between last green and first red
gh run list --repo apache/camel --branch main --workflow "Main build" \
  --limit 20 --json headSha,conclusion \
  --jq '[.[] | select(.conclusion == "success")][0].headSha'
```
Then:
```bash
git log --oneline <last-green-sha>...<failing-sha>
```

Log a summary:

```
## CI Triage (<date>)

Branch: main (SHA: <sha>)
Run: <run-id>

### Failures
- Job: <name> — Classification: <flake|regression|infra>
  Error: <one-line summary>
  Culprit: <commit sha> "<message>" (if regression)
  Action: <fix|skip|escalate>
```

### 4. Fix Regressions (Parallel Sub-agents)

For each actionable regression (max 2 per run), spawn an **implementer sub-agent**
using the Agent tool with `isolation: "worktree"`.

Each implementer agent prompt must include:

```
You are fixing a CI failure on apache/camel.

## Failure Details
- Branch: <branch>
- Job: <job-name>
- Error: <error message, truncated to relevant lines>
- Likely culprit commit: <sha> "<message>"
- CI run log (relevant excerpt): <paste relevant log lines>

## Instructions

1. Read the project rules:
   - Read CLAUDE.md (project conventions)
   - Read .oss-ai-helper-rules/project-standards.md (build/test commands)

2. Investigate the failure:
   - Read the culprit commit: git show <sha>
   - Understand what it changed and why
   - Read the failing test(s) and the code they exercise
   - Check git history for context (git log, git blame)

3. Produce the minimal fix:
   - Change only what is necessary to fix the failure
   - Do NOT refactor unrelated code
   - Do NOT disable tests or weaken assertions
   - Max 5 files changed — if more are needed, STOP and report

4. Run tests:
   - Build the affected module: mvn clean install -B -pl <module> -am
   - Run the specific failing test if identifiable
   - Report the test result

5. Format the code:
   - Run: mvn formatter:format impsort:sort -B -pl <module>

6. Return your findings:

   STATUS: <fixed|needs-escalation|cannot-fix>

   FIX_SUMMARY: <1-3 sentence description of what you changed and why>

   FILES_CHANGED:
   - <path/to/file> — <what changed>

   TEST_RESULT: <pass|fail — command + output snippet>

   RISK: <low|medium|high>

   If STATUS is needs-escalation or cannot-fix, explain why.
```

### 5. Verify Fixes (Parallel Sub-agents)

After all implementer agents complete, spawn a **verifier sub-agent** for each
fix that returned `STATUS: fixed`. Use the `loop-verifier` agent definition.

Each verifier prompt must include:

```
You are verifying a proposed CI fix for apache/camel.

## Original Failure
- Branch: <branch>
- Job: <job-name>
- Error: <error summary>

## Proposed Fix
- Files changed: <list>
- Summary: <implementer's FIX_SUMMARY>

## Your Job

1. Review the diff in the worktree — does it address the root cause?
2. Check git history — does this fix revert prior intentional work?
3. Run the tests: mvn clean install -B -pl <module> -am
4. Verify scope — are only relevant files changed? No drive-by edits?
5. Check for cheating — no disabled tests, skipped assertions, commented checks?

Return your verdict:

VERDICT: <APPROVE|REJECT|ESCALATE_HUMAN>

EVIDENCE:
- Tests: <command + result>
- Scope check: <pass/fail + notes>
- Root cause addressed: <yes/no + reasoning>

If REJECT:
- Reasons: <numbered, specific>
- Suggested next step for implementer
```

### 6. Open Draft PRs

For each fix where the verifier returned `APPROVE`:

1. Create a branch on the operator's fork:
   ```bash
   git checkout -b ci-fix/<failure-slug> origin/main
   # Apply the fix (cherry-pick from worktree or re-apply)
   git push gnodet ci-fix/<failure-slug>
   ```

2. Open a draft PR:
   ```bash
   gh pr create --repo apache/camel \
     --head gnodet:ci-fix/<failure-slug> \
     --base main \
     --draft \
     --title "ci: fix <failure-description>" \
     --body "$(cat <<'EOF'
   ## Summary

   <FIX_SUMMARY from implementer>

   ## CI Failure Details

   - Run: <run-id>
   - Job: <job-name>
   - Error: <one-line error>

   ## Verification

   - Tests: <verifier test result>
   - Scope: <verifier scope check>

   _This fix was proposed by an AI agent and should be reviewed before merging._
   _Claude Code on behalf of Guillaume Nodet_

   Co-authored-by: Claude Opus 4.6 <noreply@anthropic.com>
   EOF
   )"
   ```

For fixes where the verifier returned `REJECT`, log the rejection reason in
STATE.md and increment the attempt counter in `loop-ledger.json`.

### 7. Update State

After all fixes are proposed (or skipped):

1. Update STATE.md:
   - Set "Last Run" timestamp, branch, counts
   - Add/update entries in "Active Failures" table
   - Add new draft PRs to "Proposed Fixes" table
   - Move resolved failures to "Resolved" section
   - Add escalated failures to "Escalated" section

2. Append a run entry to `loop-run-log.md`

3. Update `loop-ledger.json` with attempt outcomes

### 8. Push State

Commit and push updated state files to the `ci-sweeper` branch on the fork:

```bash
git add STATE.md loop-run-log.md loop-ledger.json loop-budget.md
git commit -m "ci-sweeper: update state after run $(date -u +%Y-%m-%dT%H:%M:%SZ)"
git push gnodet ci-sweeper
```

If the push fails (diverged), log the error and continue — state is saved locally.
Never force-push.

### 9. Summary

Output a brief summary:

```
## CI Sweeper Complete

- Branches checked: main, camel-4.18.x
- Failures found: <N> (<M> regressions, <F> flakes, <I> infra)
- Fixes proposed: <P> draft PRs
- Escalated: <E> failures
- Verifier rejection rate: <X>%
- Next check in: 15 minutes
```

## Constraints

You MUST:
- Read STATE.md before checking CI to track across runs
- Early-exit in < 5k tokens if all branches are green
- Run loop-guard before each fix attempt
- Spawn implementer agents with worktree isolation
- Run verifier agent before opening any PR
- Open all PRs as draft on the operator's fork
- Include AI attribution in all PRs
- Update STATE.md after every run
- Respect the 2-failure-per-iteration limit

You MUST NOT:
- Merge, close, or label any PR
- Push to `apache/camel` directly
- Fix flaky tests (log and skip)
- Act on infrastructure failures (escalate)
- Disable tests or weaken assertions to go green
- Change more than 5 files per fix
- Attempt more than 3 fixes for the same failure

# CI Architecture

Overview of the GitHub Actions CI/CD ecosystem for Apache Camel.

## Workflow Overview

```
PR opened/updated
       │
       ├──► pr-id.yml ──► pr-commenter.yml (welcome message)
       │
       ├──► pr-build-main.yml (Build and test)
       │        │
       │        ├── regen.sh (build + test via Scalpel skip-tests)
       │        ├── generate test report (post-build comment)
       │        │
       │        └──► pr-test-commenter.yml (post unified comment)
       │
       └──► sonar-build.yml ──► sonar-scan.yml (SonarCloud analysis)
                                    [currently disabled — INFRA-27808]

PR comment: /component-test kafka http
       │
       └──► pr-manual-component-test.yml
                │
                └── dispatches "Build and test" with extra_modules
```

## Workflows

### `pr-build-main.yml` — Build and test

- **Trigger**: `pull_request` (main branch), `workflow_dispatch`
- **Matrix**: JDK 17, 21, 25 (25 is experimental)
- **Steps**:
  1. Check `skip-tests` label
  2. Build and test via `regen.sh` (single Maven invocation with Scalpel)
  3. Check for uncommitted generated files
  4. Generate test report from Scalpel's JSON output
  5. Upload test comment as artifact
- **Inputs** (workflow_dispatch): `pr_number`, `pr_ref`, `extra_modules`, `skip_full_build`

### `pr-test-commenter.yml` — Post CI test comment

- **Trigger**: `workflow_run` on "Build and test" completion
- **Purpose**: Posts the unified test summary comment on the PR
- **Why separate**: Uses `workflow_run` to run in base repo context, allowing comment posting on fork PRs (where `GITHUB_TOKEN` is read-only)

### `pr-manual-component-test.yml` — /component-test handler

- **Trigger**: `issue_comment` with `/component-test` prefix
- **Who**: MEMBER, OWNER, or CONTRIBUTOR only
- **What**: Resolves component names to module paths, dispatches the main "Build and test" workflow with `extra_modules` and `skip_full_build=true`
- **Build**: Uses a quick targeted build (`-Dquickly`) of the requested modules and their dependencies instead of the full `regen.sh` build

### `pr-id.yml` + `pr-commenter.yml` — Welcome message

- **Trigger**: `pull_request` (all branches)
- **Purpose**: Posts the one-time welcome message on new PRs
- **Why two workflows**: `pr-id.yml` runs in PR context (uploads PR number), `pr-commenter.yml` runs via `workflow_run` with write permissions

### `main-build.yml` — Main branch build

- **Trigger**: `push` to main, camel-4.14.x, camel-4.18.x
- **Steps**: Same as PR build but without comment posting

### `sonar-build.yml` + `sonar-scan.yml` — SonarCloud PR analysis

- **Status**: Temporarily disabled (INFRA-27808 — SonarCloud quality gate adjustment pending)
- **Trigger**: `pull_request` (main branch) → `workflow_run` on SonarBuild completion
- **Why two workflows**: `sonar-build.yml` runs in PR context (builds with JaCoCo coverage via Scalpel, uploads compiled classes artifact), `sonar-scan.yml` runs via `workflow_run` with secrets access to run the Sonar scanner and post results
- **Coverage scope**: Scalpel dynamically detects affected modules and runs tests with JaCoCo coverage on them — no hardcoded module list

### Other workflows

- `pr-labeler.yml` — Auto-labels PRs based on changed files
- `pr-doc-validation.yml` — Validates documentation changes
- `pr-cleanup-branches.yml` — Cleans up merged PR branches
- `alternative-os-build-main.yml` — Tests on non-Linux OSes
- `check-container-versions.yml` — Checks test container version updates
- `generate-sbom-main.yml` — Generates SBOM for releases
- `security-scan.yml` — Security vulnerability scanning

## Actions

### `incremental-build`

Post-build test report generator. Reads Scalpel's JSON report (`target/scalpel-report.json`) and generates a PR comment showing:

1. **Directly affected modules** with detection reasons (`SOURCE_CHANGE`, `POM_CHANGE`, `TRANSITIVE_DEPENDENCY`, `MANAGED_PLUGIN`, `TEST_CHANGE`)
2. **Changed properties, managed dependencies, managed plugins**
3. **Downstream modules** tested as dependents (collapsible)
4. **Upstream modules** compiled but with tests skipped (collapsible)
5. **Extra modules** from `/component-test`

The script also:

- Detects tests disabled in CI (`@DisabledIfSystemProperty(named = "ci.env.name")`)
- Checks for excluded modules with associated integration tests (via `manual-it-mapping.txt`) and advises contributors to run them manually
- Parses test failures from surefire/failsafe reports

### `install-mvnd`

Installs the Maven Daemon (mvnd) for faster builds.

### `install-packages`

Installs system packages required for the build.

## Scalpel: Automatic Test Selection

[Maveniverse Scalpel](https://github.com/maveniverse/scalpel) is a Maven core extension that detects which modules are affected by a PR's changes and controls test execution accordingly.

### How it works

Scalpel is configured in `.mvn/extensions.xml` and runs automatically during Maven builds. In CI, it operates in **skip-tests** mode: all modules are compiled, but tests are only executed on affected modules.

Detection capabilities:
- **Source changes**: Files changed in a module's directory
- **POM changes**: Property, dependency, and plugin changes in any `pom.xml`
- **Transitive dependencies**: Changes that propagate through the dependency graph
- **Managed dependencies**: Version changes via `<dependencyManagement>` (even without explicit `<version>` in child modules)
- **Managed plugins**: Plugin version changes via `<pluginManagement>`
- **Property indirection**: `${property}` references resolved transitively

### Configuration

Scalpel flags are set in `etc/scripts/regen.sh` when running in CI (`GITHUB_ACTIONS=true`):

| Flag | Value | Purpose |
| --- | --- | --- |
| `scalpel.mode` | `skip-tests` | Build all, test only affected |
| `scalpel.skipTestsForUpstream` | `true` | Don't test upstream-only modules |
| `scalpel.fetchBaseBranch` | `false` | Base branch pre-fetched by workflow (git CLI handles shallow clones better than JGit) |
| `scalpel.fullBuildTriggers` | *(empty)* | Override `.mvn/**` default |
| `scalpel.reportFile` | `target/scalpel-report.json` | JSON report for PR comment |
| `scalpel.impactedLog` | `target/scalpel-impacted.txt` | Simple module path list |

### Developer machines

On developer machines, Scalpel is a no-op: without `GITHUB_BASE_REF` (set by GitHub Actions for PRs), no base branch is detected and Scalpel returns immediately. Zero impact on local builds.

### Fail-safe

If Scalpel encounters an error, `failSafe=true` (the default) causes it to fall back to a full build — all tests run. This means Scalpel errors never cause false negatives (missed tests).

## PR Labels

| Label | Effect |
| --- | --- |
| `incremental-skip-tests` | Skip all tests (pass `--skip-tests` to regen.sh) |

## CI Environment

The CI sets `-Dci.env.name=github.com` via `MVND_OPTS` (in `install-mvnd`). Tests can use `@DisabledIfSystemProperty(named = "ci.env.name")` to skip flaky tests in CI. The test comment warns about these skipped tests.

## Manual Integration Test Advisories

Some integration test suites are excluded from CI. When a contributor changes a module with a mapping entry in `manual-it-mapping.txt`, CI posts an advisory in the PR comment:

> You modified `dsl/camel-jbang/camel-jbang-core`. The related integration tests in `dsl/camel-jbang/camel-jbang-it` are excluded from CI. Consider running them manually:
>
> ```
> mvn verify -f dsl/camel-jbang/camel-jbang-it -Djbang-it-test
> ```

To add new mappings, edit `manual-it-mapping.txt` using the format:

```
source-artifact-id:it-module-path:command
```

## Multi-JDK Artifact Behavior

All non-experimental JDK matrix entries (17, 21) upload the CI comment artifact with `overwrite: true`. This ensures a comment is posted even if one JDK build fails. Since the comment content is identical across JDKs (same modules are tested regardless of JDK version), last writer wins.

## Comment Markers

PR comments use HTML markers for upsert (create-or-update) behavior:

- `<!-- ci-tested-modules -->` — Unified test summary comment

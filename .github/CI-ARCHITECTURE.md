# CI Architecture

Overview of the GitHub Actions CI/CD ecosystem for Apache Camel.

## Workflow Overview

```
PR opened/updated
       │
       ├──► pr-id.yml ──► pr-commenter.yml (welcome message)
       │
       └──► pr-build-main.yml (Build and test)
                │
                ├── regen.sh (full build, no tests)
                ├── incremental-build (test affected modules)
                │       ├── File-path analysis
                │       ├── POM dependency analysis
                │       └── Extra modules (/component-test)
                │
                └──► pr-test-commenter.yml (post unified comment)

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
  1. Full build via `regen.sh` (`mvn install -DskipTests -Pregen`)
  2. Check for uncommitted generated files
  3. Run incremental tests (only affected modules)
  4. Upload test comment as artifact
- **Inputs** (workflow_dispatch): `pr_number`, `pr_ref`, `extra_modules`,
  `skip_full_build`

### `pr-test-commenter.yml` — Post CI test comment
- **Trigger**: `workflow_run` on "Build and test" completion
- **Purpose**: Posts the unified test summary comment on the PR
- **Why separate**: Uses `workflow_run` to run in base repo context, allowing
  comment posting on fork PRs (where `GITHUB_TOKEN` is read-only)

### `pr-manual-component-test.yml` — /component-test handler
- **Trigger**: `issue_comment` with `/component-test` prefix
- **Who**: MEMBER, OWNER, or CONTRIBUTOR only
- **What**: Resolves component names to module paths, dispatches the main
  "Build and test" workflow with `extra_modules` and `skip_full_build=true`
- **Build**: Uses a quick targeted build (`-Dquickly`) of the requested
  modules and their dependencies instead of the full `regen.sh` build

### `pr-id.yml` + `pr-commenter.yml` — Welcome message
- **Trigger**: `pull_request` (all branches)
- **Purpose**: Posts the one-time welcome message on new PRs
- **Why two workflows**: `pr-id.yml` runs in PR context (uploads PR number),
  `pr-commenter.yml` runs via `workflow_run` with write permissions

### `main-build.yml` — Main branch build
- **Trigger**: `push` to main, camel-4.14.x, camel-4.18.x
- **Steps**: Same as PR build but without comment posting

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
The core test runner. Determines which modules to test using:
1. **File-path analysis**: Maps changed files to Maven modules
2. **POM dependency analysis via [Scalpel](https://github.com/maveniverse/scalpel)**:
   For parent POM changes, uses Maven model comparison to detect changed
   properties, managed dependencies, managed plugins, and property indirection.
   Scalpel runs in `report` mode during `mvn validate` and outputs a JSON
   report listing affected modules.
3. **Extra modules**: Additional modules passed via `/component-test`

Results are merged, deduplicated, and tested. The script also:
- Detects tests disabled in CI (`@DisabledIfSystemProperty(named = "ci.env.name")`)
- Applies an exclusion list for generated/meta modules
- Generates a unified PR comment with all test information

### `install-mvnd`
Installs the Maven Daemon (mvnd) for faster builds.

### `install-packages`
Installs system packages required for the build.

## PR Labels

| Label | Effect |
|-------|--------|
| `skip-tests` | Skip all tests |
| `test-dependents` | Force testing dependent modules even if threshold exceeded |

## CI Environment

The CI sets `-Dci.env.name=github.com` via `MVND_OPTS` (in `install-mvnd`).
Tests can use `@DisabledIfSystemProperty(named = "ci.env.name")` to skip
flaky tests in CI. The test comment warns about these skipped tests.

## Scalpel POM Change Detection

[Scalpel](https://github.com/maveniverse/scalpel) is a Maven extension that
detects which modules are affected by POM changes using Maven model comparison
(not simple text grep). It handles:

- **Changed properties**: Compares old/new `<properties>` values
- **Managed dependencies**: Detects changes in `<dependencyManagement>` entries
- **Managed plugins**: Detects changes in `<pluginManagement>` entries
- **Property indirection**: If a managed dependency's version uses a changed
  property (e.g. `${spring.version}`), that dependency is marked as changed
- **Child module scanning**: For parent POM changes, checks each child module's
  `pom.xml` for references to changed properties, managed deps, or managed plugins

Scalpel is cloned from GitHub and built during CI. It runs as a Maven core
extension during `mvn validate` in `report` mode, outputting a JSON report
(`target/scalpel-report.json`) without modifying the reactor.

## Multi-JDK Artifact Behavior

All non-experimental JDK matrix entries (17, 21) upload the CI comment
artifact with `overwrite: true`. This ensures a comment is posted even if
one JDK build fails. Since the comment content is identical across JDKs
(same modules are tested regardless of JDK version), last writer wins.

## Comment Markers

PR comments use HTML markers for upsert (create-or-update) behavior:
- `<!-- ci-tested-modules -->` — Unified test summary comment

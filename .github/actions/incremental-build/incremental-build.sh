#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Post-build comment generator for Apache Camel PRs.
#
# Reads Scalpel's JSON report (target/scalpel-report.json) to generate
# a PR comment showing which modules were tested and why.
#
# This script runs AFTER the build+test step, which uses Scalpel's
# skip-tests mode to build all modules and test only affected ones.
# See https://github.com/maveniverse/scalpel

set -euo pipefail

# ── Utility functions ──────────────────────────────────────────────────

# Scan tested modules for @DisabledIfSystemProperty(named = "ci.env.name")
# and return a markdown warning listing affected files.
detectDisabledTests() {
  local skipped=""

  for mod_path in $@; do
    if [ -d "$mod_path" ]; then
      local matches
      matches=$(grep -rl 'DisabledIfSystemProperty' "$mod_path" --include="*.java" 2>/dev/null \
        | xargs grep -l 'ci.env.name' 2>/dev/null || true)
      if [ -n "$matches" ]; then
        local count
        count=$(echo "$matches" | wc -l | tr -d ' ')
        skipped="${skipped}\n- \`${mod_path}\`: ${count} test(s) disabled on GitHub Actions"
      fi
    fi
  done

  if [ -n "$skipped" ]; then
    echo -e "$skipped"
  fi
}

# Check if changed modules have associated integration tests excluded from CI.
# Reads manual-it-mapping.txt and appends advisories to the PR comment.
checkManualItTests() {
  local comment_file="$1"
  shift
  local module_paths="$@"
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  local mapping_file="${script_dir}/manual-it-mapping.txt"

  [[ ! -f "$mapping_file" ]] && return

  declare -A it_commands
  declare -A it_sources
  local it_found=0

  while IFS=: read -r source_id it_module command; do
    # Skip comments and empty lines
    [[ -z "$source_id" || "$source_id" == \#* ]] && continue
    source_id="${source_id// /}"
    it_module="${it_module// /}"
    command="${command#"${command%%[![:space:]]*}"}"

    # Check if any module path matches this source_id
    for module_path in $module_paths; do
      if [[ "$(basename "$module_path")" == "$source_id" ]]; then
        it_commands["$it_module"]="$command"
        it_sources["$it_module"]="${it_sources[$it_module]:-}${it_sources[$it_module]:+, }\`${module_path}\`"
        it_found=1
      fi
    done
  done < "$mapping_file"

  if [[ "$it_found" -eq 1 ]]; then
    echo "" >> "$comment_file"
    echo ":bulb: **Manual integration tests recommended:**" >> "$comment_file"
    for it_module in "${!it_sources[@]}"; do
      echo "" >> "$comment_file"
      echo "> You modified ${it_sources[$it_module]}. The related integration tests in \`${it_module}\` are excluded from CI. Consider running them manually:" >> "$comment_file"
      echo '> ```' >> "$comment_file"
      echo "> ${it_commands[$it_module]}" >> "$comment_file"
      echo '> ```' >> "$comment_file"
    done
  fi
}

# ── Comment generation ─────────────────────────────────────────────────

writeComment() {
  local comment_file="$1"
  local report="$2"
  local extra_modules="$3"

  echo "<!-- ci-tested-modules -->" > "$comment_file"

  # If the Scalpel report exists, show affected modules
  if [ -f "$report" ]; then
    # Check if full build was triggered
    local full_build
    full_build=$(jq -r '.fullBuildTriggered' "$report")
    if [ "$full_build" = "true" ]; then
      local trigger_file
      trigger_file=$(jq -r '.triggerFile // "unknown"' "$report")
      echo ":test_tube: **Full build triggered** — all modules tested" >> "$comment_file"
      echo "" >> "$comment_file"
      echo "Trigger: \`${trigger_file}\` matched full-build trigger pattern" >> "$comment_file"
    else
      # Extract affected modules by category
      local direct_count upstream_count downstream_count total_count
      direct_count=$(jq -r '[.affectedModules[] | select(.category == "DIRECT")] | length' "$report" 2>/dev/null || echo "0")
      upstream_count=$(jq -r '[.affectedModules[] | select(.category == "UPSTREAM")] | length' "$report" 2>/dev/null || echo "0")
      downstream_count=$(jq -r '[.affectedModules[] | select(.category == "DOWNSTREAM")] | length' "$report" 2>/dev/null || echo "0")
      total_count=$(jq -r '.affectedModules | length' "$report" 2>/dev/null || echo "0")

      # Section 1: Directly affected modules with reasons
      if [ "$direct_count" -gt 0 ]; then
        echo ":test_tube: **CI tested ${direct_count} directly affected module(s):**" >> "$comment_file"
        echo "" >> "$comment_file"
        jq -r '.affectedModules[] | select(.category == "DIRECT") | "- `\(.path)` (\(.reasons | join(", ")))"' "$report" >> "$comment_file"
      fi

      # Section 2: Changed properties, managed deps, managed plugins
      local changed_props managed_deps managed_plugins
      changed_props=$(jq -r '(.changedProperties // []) | if length > 0 then join(", ") else "" end' "$report" 2>/dev/null || true)
      managed_deps=$(jq -r '(.changedManagedDependencies // []) | if length > 0 then join(", ") else "" end' "$report" 2>/dev/null || true)
      managed_plugins=$(jq -r '(.changedManagedPlugins // []) | if length > 0 then join(", ") else "" end' "$report" 2>/dev/null || true)

      if [ -n "$changed_props" ] || [ -n "$managed_deps" ] || [ -n "$managed_plugins" ]; then
        echo "" >> "$comment_file"
        echo ":white_check_mark: **POM changes detected:**" >> "$comment_file"
        echo "" >> "$comment_file"
        [ -n "$changed_props" ] && echo "- Properties: ${changed_props}" >> "$comment_file"
        [ -n "$managed_deps" ] && echo "- Managed dependencies: ${managed_deps}" >> "$comment_file"
        [ -n "$managed_plugins" ] && echo "- Managed plugins: ${managed_plugins}" >> "$comment_file"
      fi

      # Section 3: Downstream modules (tested as dependents)
      if [ "$downstream_count" -gt 0 ]; then
        echo "" >> "$comment_file"
        echo "<details><summary>Downstream modules also tested (${downstream_count})</summary>" >> "$comment_file"
        echo "" >> "$comment_file"
        jq -r '.affectedModules[] | select(.category == "DOWNSTREAM") | "- `\(.path)`"' "$report" >> "$comment_file"
        echo "" >> "$comment_file"
        echo "</details>" >> "$comment_file"
      fi

      # Section 4: Upstream modules (compiled only, tests skipped)
      if [ "$upstream_count" -gt 0 ]; then
        echo "" >> "$comment_file"
        echo "<details><summary>Upstream modules compiled (tests skipped) (${upstream_count})</summary>" >> "$comment_file"
        echo "" >> "$comment_file"
        jq -r '.affectedModules[] | select(.category == "UPSTREAM") | "- `\(.path)`"' "$report" >> "$comment_file"
        echo "" >> "$comment_file"
        echo "</details>" >> "$comment_file"
      fi

      # Empty state (no Scalpel-detected modules)
      if [ "$total_count" -eq 0 ] && [ -z "$extra_modules" ]; then
        echo ":information_source: No affected modules detected — no targeted tests were run." >> "$comment_file"
      fi
    fi
  fi

  # Section: Extra modules (from /component-test) — shown regardless of Scalpel report
  if [ -n "$extra_modules" ]; then
    echo "" >> "$comment_file"
    echo ":heavy_plus_sign: **Additional modules tested** (via \`/component-test\`):" >> "$comment_file"
    echo "" >> "$comment_file"
    for w in $(echo "$extra_modules" | tr ',' '\n'); do
      echo "- \`$w\`" >> "$comment_file"
    done
  fi

  # If nothing at all was shown
  if [ ! -f "$report" ] && [ -z "$extra_modules" ]; then
    echo ":information_source: CI did not produce a change detection report." >> "$comment_file"
  fi

  # Attribution
  if [ -f "$report" ]; then
    echo "" >> "$comment_file"
    echo "> :microscope: Detected via [Maveniverse Scalpel](https://github.com/maveniverse/scalpel) (\`skip-tests\` mode)" >> "$comment_file"
  fi
}

# ── Main ───────────────────────────────────────────────────────────────

main() {
  local extraModules="${1:-}"
  local report="target/scalpel-report.json"
  local comment_file="incremental-test-comment.md"
  local build_failed="${BUILD_FAILED:-false}"

  echo "Generating test report..."

  # Generate PR comment from Scalpel report
  writeComment "$comment_file" "$report" "$extraModules"

  # Get tested module paths (DIRECT + DOWNSTREAM) for disabled-test and IT scanning
  local tested_paths=""
  if [ -f "$report" ]; then
    tested_paths=$(jq -r '.affectedModules[] | select(.category == "DIRECT" or .category == "DOWNSTREAM") | .path' "$report" 2>/dev/null || true)
  fi
  # Include extra modules in the scan
  if [ -n "$extraModules" ]; then
    tested_paths="$tested_paths $(echo "$extraModules" | tr ',' ' ')"
  fi

  # Check for tests disabled in CI
  if [ -n "$tested_paths" ]; then
    local disabled_tests
    disabled_tests=$(detectDisabledTests $tested_paths)
    if [ -n "$disabled_tests" ]; then
      echo "" >> "$comment_file"
      echo ":warning: **Some tests are disabled on GitHub Actions** (\`@DisabledIfSystemProperty(named = \"ci.env.name\")\`) and require manual verification:" >> "$comment_file"
      echo "$disabled_tests" >> "$comment_file"
    fi

    # Check for excluded IT suites that should be run manually
    checkManualItTests "$comment_file" $tested_paths
  fi

  # Write step summary
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ] && [ -f "$report" ]; then
    {
      echo ""
      echo "### Tested modules"
      echo ""
      jq -r '.affectedModules[] | select(.category == "DIRECT" or .category == "DOWNSTREAM") | "- `\(.path)` [\(.category)] \(.reasons | join(", "))"' "$report" 2>/dev/null || true
      echo ""
    } >> "$GITHUB_STEP_SUMMARY"
  fi

  # Parse test failures from surefire/failsafe reports
  if [ "$build_failed" = "true" ]; then
    echo "Processing surefire and failsafe reports to create the summary"
    if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
      echo -e "| Failed Test | Duration | Failure Type |\n| --- | --- | --- |" >> "$GITHUB_STEP_SUMMARY"
    fi
    find . -path '*target/*-reports*' -iname '*.txt' -exec .github/actions/incremental-build/parse_errors.sh {} \;
  fi

  echo "Test report generated: $comment_file"
}

main "$@"

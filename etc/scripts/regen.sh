#!/bin/sh
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

set -e

# Move to top directory
cd `dirname "$0"`/../..

# Parse arguments
skip_tests=""
coverage=""
for arg in "$@"; do
  case "$arg" in
    --skip-tests) skip_tests=true ;;
    --coverage)   coverage=true ;;
  esac
done

# Force clean
git clean -fdx
rm -Rf **/src/generated/

# Build flags: Scalpel skip-tests mode in CI
# On developer machines, Scalpel auto-disables (no GITHUB_BASE_REF).
scalpel_flags=""
if [ "${GITHUB_ACTIONS:-}" = "true" ] && [ -z "$skip_tests" ]; then
  scalpel_flags="-Dscalpel.mode=skip-tests -Dscalpel.skipTestsForUpstream=true -Dscalpel.fetchBaseBranch=false -Dscalpel.fullBuildTriggers= -Dscalpel.reportFile=target/scalpel-report.json -Dscalpel.impactedLog=target/scalpel-impacted.txt"
fi

extra_flags=""
if [ -n "$skip_tests" ]; then
  extra_flags="-DskipTests"
fi
if [ -n "$coverage" ]; then
  extra_flags="$extra_flags -Dcoverage"
fi

# Regenerate everything (and run tests on affected modules via Scalpel in CI)
if ./mvnw --batch-mode -Pregen $scalpel_flags $extra_flags install >> build.log 2>&1; then
  echo "mvn -Pregen succeeded."
else
  echo "mvn -Pregen failed. Last 50 lines of build.log:"
  tail -n 50 build.log
  exit 1
fi

# One additional pass to get the info for the 'others' jars
if ./mvnw --batch-mode install -f catalog/camel-catalog >> build.log 2>&1; then
  echo "mvn install for camel-catalog succeeded."
else
  echo "mvn install for camel-catalog failed. Last 50 lines of build.log:"
  tail -n 50 build.log
  exit 1
fi

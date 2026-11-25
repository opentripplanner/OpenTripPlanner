#!/usr/bin/env bash
# Compliance harness for the OTP modularization. See modularization/plan.md.
#
#   harness.sh capture   <otp-shaded.jar> <out-dir> [data-dir]
#   harness.sh compare   <baseline-dir> <candidate-dir>
#   harness.sh selfcheck <otp-shaded.jar> <work-dir>
#
# capture   builds a graph with the jar, starts OTP, sends every scenario and writes the answers.
# compare   compares two captures against approved-discrepancies.json (exit 1 on differences).
# selfcheck captures twice with the same jar and compares. It must report no differences. It tests
#           that the harness and the scenarios are deterministic.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
DEFAULT_DATA="$REPO_ROOT/application/src/test/resources/portland"
APPROVED="$HERE/approved-discrepancies.json"

build_harness() {
  if [ ! -f "$HERE/target/compliance-harness-1.0-SNAPSHOT.jar" ] || \
     [ -n "$(find "$HERE/src" "$HERE/pom.xml" -newer "$HERE/target/compliance-harness-1.0-SNAPSHOT.jar" -print -quit)" ]; then
    mvn -q -f "$HERE/pom.xml" package -DskipTests
  fi
}

run() {
  build_harness
  (cd "$HERE" && java -jar target/compliance-harness-1.0-SNAPSHOT.jar "$@")
}

abs() { (cd "$(dirname "$1")" && echo "$PWD/$(basename "$1")"); }

case "${1:-}" in
  capture)
    run capture --jar "$(abs "$2")" --out "$(abs "$3")" --data "${4:-$DEFAULT_DATA}"
    ;;
  compare)
    run compare --baseline "$(abs "$2")" --candidate "$(abs "$3")" --approved "$APPROVED"
    ;;
  selfcheck)
    mkdir -p "$3"
    "$0" capture "$2" "$3/run-1"
    "$0" capture "$2" "$3/run-2"
    "$0" compare "$3/run-1" "$3/run-2"
    ;;
  *)
    sed -n '2,13p' "$0"
    exit 2
    ;;
esac

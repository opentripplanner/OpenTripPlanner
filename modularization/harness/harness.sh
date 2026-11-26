#!/usr/bin/env bash
# Compliance harness for the OTP modularization. See modularization/plan.md.
#
#   harness.sh capture   <otp-shaded.jar> <out-dir> [dataset,dataset,...]
#   harness.sh serve     <otp-shaded.jar> <dataset>
#   harness.sh compare   <baseline-dir> <candidate-dir>
#   harness.sh selfcheck <otp-shaded.jar> <work-dir>
#
# capture   for each dataset (see datasets/): builds a graph with the jar from the dataset inputs,
#           starts OTP, sends every scenario of the dataset and writes the answers. Also captures the
#           generated configuration documentation (doc/user) of the tree in OTP_TREE (default: this
#           checkout), which must be the tree the jar was built from.
# serve     builds the graph of one dataset and keeps OTP running on port 8080 (Ctrl-C to stop),
#           for exploring the data and writing scenarios.
# compare   compares two captures against approved-discrepancies.json (exit 1 on differences).
# selfcheck captures twice with the same jar and compares. It must report no differences. It tests
#           that the harness and the scenarios are deterministic.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
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
    only=()
    [ -n "${4:-}" ] && only=(--only "$4")
    run capture --jar "$(abs "$2")" --out "$(abs "$3")" --repo-root "$REPO_ROOT" ${only[@]+"${only[@]}"}
    # The documentation comes from the source tree that built the jar (OTP_TREE), by default this one.
    run docs --tree "$(cd "${OTP_TREE:-$REPO_ROOT}" && pwd)" --out "$(abs "$3")/config-docs"
    ;;
  serve)
    run serve --jar "$(abs "$2")" --repo-root "$REPO_ROOT" --only "$3"
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
    sed -n '2,16p' "$0"
    exit 2
    ;;
esac

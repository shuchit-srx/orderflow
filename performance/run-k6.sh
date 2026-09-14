#!/usr/bin/env bash
set -euo pipefail

TEST="${1:-smoke}"

case "$TEST" in
  smoke)
    SCRIPT="performance/k6/smoke.js"
    ;;
  load)
    SCRIPT="performance/k6/load.js"
    ;;
  authenticated-load)
    SCRIPT="performance/k6/authenticated-load.js"
    ;;
  stress)
    SCRIPT="performance/k6/stress.js"
    ;;
  order-write)
    SCRIPT="performance/k6/order-write.js"
    ;;
  *)
    echo "Usage: $0 {smoke|load|authenticated-load|stress|order-write}"
    exit 1
    ;;
esac

mkdir -p performance/reports

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
REPORT="performance/reports/${TEST}-${TIMESTAMP}.json"

k6 run \
  --include-system-env-vars \
  -e SUMMARY_FILE="$REPORT" \
  --summary-mode=full \
  --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99),count" \
  "$SCRIPT"

echo "$REPORT"
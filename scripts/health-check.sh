#!/usr/bin/env sh
set -eu

for port in 8080 8081 8082 8083 8084; do
  echo "Checking http://localhost:${port}/actuator/health"
  curl --fail --silent "http://localhost:${port}/actuator/health"
  echo
done

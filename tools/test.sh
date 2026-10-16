#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_output=$(mktemp -d)
trap 'rm -rf "$test_output"' EXIT
java com.sun.tools.javac.Main -d "$test_output" app/src/main/java/kr/local/timedclick/MonthlyTime.java tests/MonthlyTimeTest.java
java -cp "$test_output" MonthlyTimeTest

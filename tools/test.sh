#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_output=$(mktemp -d)
trap 'rm -rf "$test_output"' EXIT
java com.sun.tools.javac.Main -d "$test_output" app/src/main/java/kr/local/timedclick/MonthlyTime.java app/src/main/java/kr/local/timedclick/RunTiming.java tests/MonthlyTimeTest.java tests/RunTimingTest.java
java -cp "$test_output" MonthlyTimeTest
java -cp "$test_output" RunTimingTest
java com.sun.tools.javac.Main -d "$test_output" tests/fakes/android/view/accessibility/AccessibilityNodeInfo.java app/src/main/java/kr/local/timedclick/AccessibilityMatcher.java tests/AccessibilityMatcherTest.java
java -cp "$test_output" kr.local.timedclick.AccessibilityMatcherTest


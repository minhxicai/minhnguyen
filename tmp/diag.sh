#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
echo "PWD=$PWD"
echo "=== java ==="
java -version 2>&1 | head -2
echo "=== gradle_home ==="
ls -ld ~/.gradle 2>&1
echo "=== gradle version ==="
bash tmp/gradle-8.7/bin/gradle -v 2>&1 | head -15
echo "GRADLE_RC=$?"
echo "=== sdk platforms ==="
ls tmp/sdk 2>&1
echo "=== sdkmanager file size/lines ==="
wc -lc tmp/sdk/cmdline-tools/latest/bin/sdkmanager
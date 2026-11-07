#!/usr/bin/env bash
set -x
java -Xmx256m -cp "tmp/gradle-8.7/lib/*" org.gradle.launcher.GradleMain --version
echo RC=$?

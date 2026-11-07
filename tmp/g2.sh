#!/usr/bin/env bash
cd .||exit 1
export GRADLE_USER_HOME="$PWD/tmp/gradle-home"
java -Xmx2048m -cp "tmp/gradle-8.7/lib/*" org.gradle.launcher.GradleMain "$@"
echo JAVA_RC=$?

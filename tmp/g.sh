#!/usr/bin/env bash
# Gọi Gradle trực tiếp qua java launcher, bỏ qua shell script wrapper
cd . || exit 1
export GRADLE_USER_HOME="$PWD/tmp/gradle-home"
CP="tmp/gradle-8.7/lib/*"
exec java -Xmx2048m -cp "$CP" org.gradle.launcher.GradleMain "$@"

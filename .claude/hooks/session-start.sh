#!/bin/bash
# SessionStart hook for Claude Code cloud sessions.
#
# Forge 1.20.1 needs JDK 17, which the cloud image does not ship (it has JDK 21).
# This installs it and then warms the Gradle caches (Minecraft, Forge and the
# mapped jars), so that builds and tests work right away. The container state is
# cached after the hook finishes, so later sessions start quickly.
#
# Idempotent and non-interactive. Runs only in cloud sessions.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

# 1) JDK 17 (Gradle finds it through its toolchain auto-detection in /usr/lib/jvm)
if ! dpkg -s openjdk-17-jdk-headless >/dev/null 2>&1; then
  echo "[session-start] installing openjdk-17-jdk-headless"
  export DEBIAN_FRONTEND=noninteractive
  $SUDO apt-get update -qq
  $SUDO apt-get install -y -qq openjdk-17-jdk-headless
else
  echo "[session-start] openjdk-17-jdk-headless already installed"
fi

# 2) Warm the Gradle caches. A failure here (for example no network) must not block the session.
cd "${CLAUDE_PROJECT_DIR:-$(pwd)}"
echo "[session-start] warming Gradle caches (first run takes a few minutes)"
if ./gradlew --no-daemon -q compileJava compileTestJava compileGametestJava >/tmp/yperite-session-start-gradle.log 2>&1; then
  echo "[session-start] Gradle caches are ready"
else
  echo "[session-start] WARNING: Gradle warm-up failed; see /tmp/yperite-session-start-gradle.log" >&2
fi

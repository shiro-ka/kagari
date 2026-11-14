#!/usr/bin/env bash
set -euo pipefail

apk_dir="$1"
out="$2"
package="lab49.kagari"
activity="dev.dimension.flare.MainActivity"

mkdir -p "$out"
apk=$(ls "$apk_dir"/*.apk | head -n 1)

adb install -r "$apk"
adb logcat -c
adb shell am start -W -n "$package/$activity" | tee "$out/am-start.txt"
sleep 20
adb exec-out screencap -p > "$out/01-launch.png"
if adb shell pidof "$package" > "$out/pid.txt"; then
  echo "running" >> "$out/pid.txt"
else
  echo "not running" > "$out/pid.txt"
fi
if [ -n "${BSKY_HANDLE:-}" ]; then
  python3 .github/kagari/emulator_login.py "$out/login" || echo "login script failed" > "$out/login-failed.txt"
fi
adb logcat -d > "$out/logcat.txt"
adb logcat -d -b crash > "$out/crash.txt" || true
if grep -q "lab49.kagari" "$out/crash.txt" || ! grep -qx running "$out/pid.txt"; then
  echo "app crashed or is not running" >&2
  exit 1
fi

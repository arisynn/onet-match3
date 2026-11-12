#!/usr/bin/env bash
set -euo pipefail
mkdir -p build-reports
adb logcat -c
adb install -r android/build/outputs/apk/debug/android-debug.apk
adb install -r android/build/outputs/apk/release/android-release.apk
for package in com.onetmatch3.game.debug com.onetmatch3.game; do
  adb shell am force-stop "$package"
  adb shell am start -W -n "$package/com.onetmatch3.game.AndroidLauncher"
  sleep 8
  adb logcat -d > "build-reports/$package-logcat.txt"
  adb shell pidof "$package"
  adb exec-out screencap -p > "build-reports/$package.png"
  if [ "$package" = com.onetmatch3.game.debug ]; then
    grep -q 'Render: screen=MAIN_MENU' "build-reports/$package-logcat.txt"
    ! grep -q 'FATAL EXCEPTION' "build-reports/$package-logcat.txt"
  fi
  adb logcat -c
 done

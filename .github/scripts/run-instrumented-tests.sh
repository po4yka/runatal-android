#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SERIAL:?The emulator runner must select the test device}"

capture_failure_diagnostics() {
    local status=$?
    trap - EXIT
    if (( status != 0 )); then
        local output_dir=app/build/reports/emulator-diagnostics
        mkdir -p "$output_dir" || exit "$status"
        # Collect evidence while the device is alive without replacing the original failure.
        timeout 30s adb -s "$ANDROID_SERIAL" logcat -d -v threadtime > "$output_dir/logcat.txt" 2>&1 || true
        timeout 30s adb -s "$ANDROID_SERIAL" shell am get-current-user > "$output_dir/current-user.txt" 2>&1 || true
        timeout 30s adb -s "$ANDROID_SERIAL" shell pm list users > "$output_dir/users.txt" 2>&1 || true
        timeout 30s adb -s "$ANDROID_SERIAL" shell pm list packages -U com.po4yka.runatal \
            > "$output_dir/packages.txt" 2>&1 || true
        timeout 30s adb -s "$ANDROID_SERIAL" shell pm list instrumentation \
            > "$output_dir/instrumentation.txt" 2>&1 || true
        timeout 30s adb -s "$ANDROID_SERIAL" shell df -h /data > "$output_dir/data-storage.txt" 2>&1 || true
    fi
    exit "$status"
}
trap capture_failure_diagnostics EXIT

diagnostics_dir=app/build/reports/emulator-diagnostics
mkdir -p "$diagnostics_dir"
timeout 30s adb -s "$ANDROID_SERIAL" logcat -b crash -d -v threadtime \
    > "$diagnostics_dir/platform-crash-log.txt" 2>&1
timeout 30s adb -s "$ANDROID_SERIAL" shell service check activity \
    > "$diagnostics_dir/platform-activity-service.txt" 2>&1
timeout 30s adb -s "$ANDROID_SERIAL" shell service check package \
    > "$diagnostics_dir/platform-package-service.txt" 2>&1
timeout 30s adb -s "$ANDROID_SERIAL" shell am get-current-user \
    > "$diagnostics_dir/platform-current-user.txt" 2>&1

python3 - <<'PY'
from pathlib import Path
import re

diagnostics = Path("app/build/reports/emulator-diagnostics")
crashes = (diagnostics / "platform-crash-log.txt").read_text()
critical_process = r"(?:surfaceflinger|system_server|zygote64|zygote)"
fatal = re.search(
    rf"(?:Fatal signal[^\n]*\b{critical_process}\b|"
    rf">>>[^\n]*\b{critical_process}\b|Process:\s*system_server\b)",
    crashes,
)
if fatal:
    raise SystemExit(f"Android platform crashed before tests: {fatal.group(0)}")
for service in ("activity", "package"):
    state = (diagnostics / f"platform-{service}-service.txt").read_text()
    if f"Service {service}: found" not in state:
        raise SystemExit(f"Required Android {service} service is unavailable: {state.strip()}")
current_user = (diagnostics / "platform-current-user.txt").read_text().strip()
if not current_user.isdigit():
    raise SystemExit(f"Android ActivityManager cannot resolve current user: {current_user}")
print(f"Android platform preflight passed: core services available, current user={current_user}")
PY

./gradlew testDebugUnitTest connectedDebugAndroidTest --info --stacktrace --no-daemon

python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

reports = sorted(Path("app/build/outputs/androidTest-results/connected/debug").glob("TEST-*.xml"))
if not reports:
    raise SystemExit("Instrumented tests produced no JUnit XML reports")

cases = failures = errors = skipped = 0
summary_has_failures = False
for report in reports:
    root = ET.parse(report).getroot()
    test_cases = list(root.iter("testcase"))
    cases += len(test_cases)
    failures += sum(case.find("failure") is not None for case in test_cases)
    errors += sum(case.find("error") is not None for case in test_cases)
    skipped += sum(case.find("skipped") is not None for case in test_cases)
    summary_has_failures |= any(
        int(suite.get(attribute, "0")) > 0
        for suite in root.iter()
        if suite.tag in ("testsuite", "testsuites")
        for attribute in ("failures", "errors", "skipped")
    )

print(f"Instrumented JUnit results: tests={cases}, failures={failures}, errors={errors}, skipped={skipped}")
if cases == 0 or failures or errors or skipped or summary_has_failures:
    raise SystemExit("Instrumented tests must execute actual cases with no failures, errors, or skips")
PY

./gradlew jacocoProjectCoverageReport --info --stacktrace --no-daemon

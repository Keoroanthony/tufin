#!/usr/bin/env bash
# start-policy-rule-engine.sh
# Builds (if necessary) and starts the Policy Rule Engine Spring Boot application,
# then verifies it comes up healthy before returning.

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
readonly JAR_PATTERN="build/libs/tufin-*.jar"
readonly LOG_DIR="logs"
readonly LOG_FILE="${LOG_DIR}/app.log"
readonly PID_FILE="${LOG_DIR}/app.pid"
# /api/v1/rules is a lightweight GET that returns HTTP 200 once the app is ready.
# The project does not include spring-boot-starter-actuator, so /actuator/health
# is not available.
readonly HEALTH_URL="http://localhost:8080/api/v1/rules"
readonly HEALTH_POLL_INTERVAL=2   # seconds between each health check poll
readonly HEALTH_TIMEOUT=60        # maximum seconds to wait for a healthy response
readonly LOG_TAIL_LINES=50        # lines of log to display on startup failure
readonly GRADLE_WRAPPER="./gradlew"

# ---------------------------------------------------------------------------
# Ensure we are always running from the project root (where gradlew lives)
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# ---------------------------------------------------------------------------
# Helper: build the JAR using the Gradle wrapper
# ---------------------------------------------------------------------------
build_jar() {
    echo "No JAR found matching '${JAR_PATTERN}'. Attempting to build with Gradle ..."

    if [[ ! -x "${GRADLE_WRAPPER}" ]]; then
        echo "ERROR: Gradle wrapper '${GRADLE_WRAPPER}' not found or not executable." >&2
        echo "       Run 'chmod +x gradlew' and retry." >&2
        exit 1
    fi

    "${GRADLE_WRAPPER}" bootJar

    echo "Build completed."
}

# ---------------------------------------------------------------------------
# Locate the JAR (build first if absent)
# ---------------------------------------------------------------------------
jar_files=( ${JAR_PATTERN} )   # word-split the glob on purpose

if [[ ${#jar_files[@]} -eq 0 || ! -f "${jar_files[0]}" ]]; then
    build_jar
    # Re-expand the glob after the build
    jar_files=( ${JAR_PATTERN} )
fi

# After a potential build, verify we actually have a file now
if [[ ${#jar_files[@]} -eq 0 || ! -f "${jar_files[0]}" ]]; then
    echo "ERROR: Build succeeded but still no JAR found matching '${JAR_PATTERN}'." >&2
    exit 1
fi

if [[ ${#jar_files[@]} -gt 1 ]]; then
    echo "WARNING: Multiple JARs found; using '${jar_files[0]}'." >&2
fi

readonly JAR_FILE="${jar_files[0]}"

# ---------------------------------------------------------------------------
# Prepare the logs directory
# ---------------------------------------------------------------------------
mkdir -p "${LOG_DIR}"

# ---------------------------------------------------------------------------
# Start the application in the background
# ---------------------------------------------------------------------------
echo "Starting Policy Rule Engine from '${JAR_FILE}' ..."
java -jar "${JAR_FILE}" >> "${LOG_FILE}" 2>&1 &
readonly APP_PID=$!

# Persist the PID so a companion stop script can reference it later
echo "${APP_PID}" > "${PID_FILE}"
echo "Process started with PID ${APP_PID}. Logs: ${LOG_FILE}"

# ---------------------------------------------------------------------------
# Poll the health endpoint until the app responds HTTP 200 or timeout expires
# ---------------------------------------------------------------------------
echo "Waiting for application to become healthy (timeout: ${HEALTH_TIMEOUT}s) ..."

elapsed=0
healthy=false

while [[ ${elapsed} -lt ${HEALTH_TIMEOUT} ]]; do
    # Verify the process is still alive before each poll — fail fast if it crashed
    if ! kill -0 "${APP_PID}" 2>/dev/null; then
        echo "ERROR: Application process (PID ${APP_PID}) terminated unexpectedly." >&2
        break
    fi

    # curl returns the HTTP status code; suppress all other output
    http_status=$(curl --silent --output /dev/null --write-out "%{http_code}" \
                       --max-time 2 "${HEALTH_URL}" 2>/dev/null || true)

    if [[ "${http_status}" == "200" ]]; then
        healthy=true
        break
    fi

    sleep "${HEALTH_POLL_INTERVAL}"
    elapsed=$(( elapsed + HEALTH_POLL_INTERVAL ))
done

# ---------------------------------------------------------------------------
# Report outcome
# ---------------------------------------------------------------------------
if [[ "${healthy}" == true ]]; then
    echo "------------------------------------------------------------"
    echo "  Policy Rule Engine started successfully."
    echo "  PID : ${APP_PID}"
    echo "  Log : ${LOG_FILE}"
    echo "------------------------------------------------------------"
    exit 0
else
    echo "ERROR: Application did not become healthy within ${HEALTH_TIMEOUT} seconds." >&2
    echo "       Last ${LOG_TAIL_LINES} lines of ${LOG_FILE}:" >&2
    echo "------------------------------------------------------------" >&2
    tail -n "${LOG_TAIL_LINES}" "${LOG_FILE}" >&2
    echo "------------------------------------------------------------" >&2
    exit 1
fi

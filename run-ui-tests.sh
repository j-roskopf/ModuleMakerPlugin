#!/usr/bin/env bash
set -euo pipefail

# Start the IDE with the robot server in the background.
# runIdeForUiTests never exits on its own, so we launch it as a background process
# and kill it when the tests finish.
echo "Starting IDE with robot server..."
./gradlew runIdeForUiTests &
IDE_PID=$!

cleanup() {
  echo "Stopping IDE (pid $IDE_PID)..."
  kill "$IDE_PID" 2>/dev/null || true
}
trap cleanup EXIT

# Wait for the robot server TCP port to accept connections (up to 5 minutes).
# The server has no GET health endpoint; nc is the reliable way to probe the port.
echo "Waiting for robot server on port 8082..."
MAX_ATTEMPTS=60
ATTEMPT=0
until nc -z localhost 8082 2>/dev/null; do
  ATTEMPT=$((ATTEMPT + 1))
  if [ "$ATTEMPT" -ge "$MAX_ATTEMPTS" ]; then
    echo "ERROR: robot server did not start after $((MAX_ATTEMPTS * 5)) seconds."
    exit 1
  fi
  echo "  attempt $ATTEMPT/$MAX_ATTEMPTS — not ready yet, retrying in 5s..."
  sleep 5
done
echo "IDE is ready!"

# Clean up any previously created test module before running.
TEST_MODULE_DIR="src/uiTest/testProject/repository"
rm -rf "$TEST_MODULE_DIR"

# Run the tests.  The EXIT trap above will kill the IDE when this exits.
./gradlew uiTest
TEST_EXIT=$?

# Clean up the created test module and restore settings.gradle.kts after running.
rm -rf "$TEST_MODULE_DIR"
git checkout -- src/uiTest/testProject/settings.gradle.kts

exit $TEST_EXIT

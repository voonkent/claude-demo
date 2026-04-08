#!/bin/bash
INPUT=$(cat)
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Only gate on git commit commands
if echo "$CMD" | grep -qE "git\s+commit"; then
  OUTPUT=$(./gradlew detekt --no-daemon 2>&1)
  EXIT_CODE=$?
  if [ $EXIT_CODE -ne 0 ]; then
    echo "DETEKT ISSUES (commit blocked):" >&2
    echo "$OUTPUT" | grep -E "^\s*(w|e) -- |> Task|FAILED|warning:|error:" | head -20 >&2
    exit 2
  fi
fi

exit 0

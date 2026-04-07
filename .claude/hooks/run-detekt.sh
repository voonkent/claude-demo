#!/bin/bash
INPUT=$(cat)
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Only gate on git commit commands
if echo "$CMD" | grep -qE "^git\s+commit"; then
  OUTPUT=$(./gradlew detekt --no-daemon -q 2>&1)
  EXIT_CODE=$?
  if [ $EXIT_CODE -ne 0 ]; then
    echo "DETEKT ISSUES:" >&2
    echo "$OUTPUT" | tail -8 >&2
    exit 2
  fi
fi

exit 0

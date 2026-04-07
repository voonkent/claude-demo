#!/bin/bash
INPUT=$(cat)
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Only gate on git commit commands
if echo "$CMD" | grep -qE "^git\s+commit"; then
  OUTPUT=$(./gradlew detekt --no-daemon -q 2>&1 | tail -8)
    if [ $? -ne 0 ]; then
      echo "DETEKT ISSUES:" >&2
      echo "$OUTPUT" >&2
      exit 2
    fi
fi

exit 0
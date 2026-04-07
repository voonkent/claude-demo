#!/bin/bash
INPUT=$(cat)
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Only gate on git commit commands
if echo "$CMD" | grep -qE "^git\s+commit"; then
  OUTPUT=$(./gradlew test --no-daemon -q 2>&1 | tail -10)
  if [ $? -ne 0 ]; then
    echo "TESTS FAILED — fix before committing:" >&2
    echo "$OUTPUT" >&2
    exit 2
  fi
fi

exit 0
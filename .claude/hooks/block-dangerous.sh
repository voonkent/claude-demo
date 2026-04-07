#!/bin/bash
INPUT=$(cat)

if ! command -v jq &>/dev/null; then
  echo "Error: 'jq' is not installed. Cannot verify command safety. Install jq to enable protection: https://jqlang.org/download/" >&2
  exit 2
fi

CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

BLOCKED_PATTERNS=(
  "rm -rf"
  "git reset --hard"
  "git push.*--force"
  "DROP TABLE"
  "DROP DATABASE"
  "TRUNCATE"
  "docker rm -f"
  "kubectl delete"
)

for pattern in "${BLOCKED_PATTERNS[@]}"; do
  if echo "$CMD" | grep -qEi "$pattern"; then
    echo "⚠️  Dangerous command detected!" >&2
    echo "   Pattern : $pattern" >&2
    echo "   Command : $CMD" >&2
    echo "BLOCKED: '$CMD' matches dangerous pattern '$pattern'." >&2
    exit 2
  fi
done

exit 0
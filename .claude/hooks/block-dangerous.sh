#!/bin/bash
INPUT=$(cat)

if ! command -v jq &>/dev/null; then
  echo "WARNING: 'jq' is not installed. Cannot verify command safety. Install jq to enable protection: https://jqlang.org/download/" >&2
  exit 1
fi

CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

BLOCKED_PATTERNS=(
  "rm -rf"
  "git reset --hard"
  "git push.*--force"
  "DROP TABLE"
  "DROP DATABASE"
  "TRUNCATE"
  "curl.*|.*sh"
  "docker rm -f"
  "kubectl delete"
  "flyway clean"
  "flyway repair"
)

for pattern in "${BLOCKED_PATTERNS[@]}"; do
  if echo "$CMD" | grep -qEi "$pattern"; then
    echo "BLOCKED: '$CMD' matches dangerous pattern '$pattern'. Use a safer alternative." >&2
    exit 2
  fi
done

exit 0
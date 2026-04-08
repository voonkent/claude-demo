---
name: jira-update-ticket
description: Update a Jira ticket -- transition status, update fields, or add comments. Use when the user wants to modify a Jira issue.
---

Update a Jira Cloud ticket by performing status transitions, field updates, or adding comments.

**Input**: A ticket key (e.g., `PROJ-123`) and the desired action (transition, update fields, or add comment).

**Note**: Environment variables (`JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_BASE_URL`) are validated by the orchestrating agent before this skill is invoked.

---

## Steps

### Step 1 -- Validate input

**Ticket key validation:**
- Ensure a ticket key was provided and matches `[A-Z][A-Z0-9]+-[0-9]+` (e.g., `PROJ-123`).
- If the ticket key is missing, use `AskUserQuestion` to prompt the user for it.

**Determine the operation type:**
- **Transition** -- user wants to change status (e.g., "move to In Progress")
- **Update fields** -- user wants to change summary, priority, assignee, labels
- **Add comment** -- user wants to post a comment

**For add comment operations:**
- If the comment text is missing or empty, use `AskUserQuestion` to prompt the user for the comment content.
- Do not proceed with the API call until both ticket key and comment text are provided.

### Step 2a -- Transition status

First, fetch available transitions:
```bash
curl -s -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -H "Accept: application/json" \
  "${JIRA_BASE_URL}/rest/api/3/issue/<KEY>/transitions"
```

Show the available transitions to confirm the correct one. Then apply:
```bash
curl -s -w "\n%{http_code}" -X POST \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"transition": {"id": "<TRANSITION_ID>"}}' \
  "${JIRA_BASE_URL}/rest/api/3/issue/<KEY>/transitions"
```

### Step 2b -- Update fields

```bash
curl -s -w "\n%{http_code}" -X PUT \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"fields": {"summary": "New summary", "priority": {"name": "High"}}}' \
  "${JIRA_BASE_URL}/rest/api/3/issue/<KEY>"
```

Adjust the JSON body to include only the fields being updated. Common fields:
- `summary` -- string
- `priority` -- `{"name": "High|Medium|Low"}`
- `assignee` -- `{"accountId": "<ACCOUNT_ID>"}` or `null` to unassign
- `labels` -- array of strings

### Step 2c -- Add comment

**Before making the API call:**
1. Confirm the ticket key and comment text are both provided.
2. Show the user what will be posted: "Ready to add comment to **<KEY>**: '<COMMENT_TEXT>'"
3. Await implicit confirmation (or ask explicitly if needed).

**Make the API call:**
```bash
curl -s -w "\n%{http_code}" -X POST \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"body": {"type": "doc", "version": 1, "content": [{"type": "paragraph", "content": [{"type": "text", "text": "<COMMENT_TEXT>"}]}]}}' \
  "${JIRA_BASE_URL}/rest/api/3/issue/<KEY>/comment"
```

### Step 3 -- Handle errors

| Status | Action |
|--------|--------|
| `200`/`204` | Success -- confirm the update to the user |
| `400` | Bad request -- check the JSON payload format |
| `401` | Auth failed -- verify credentials |
| `403` | Permission denied -- user may lack edit access |
| `404` | Ticket not found -- verify the key |
| Other | Report the status code and response body |

### Step 4 -- Confirm result

After a successful update, report what was changed:
- For transitions: "PROJ-123 moved from **To Do** to **In Progress**"
- For field updates: "PROJ-123 priority updated to **High**"
- For comments: Parse the `self` field from the JSON response to get the comment ID, then construct the browsable URL:
  `${JIRA_BASE_URL}/browse/<KEY>?focusedCommentId=<COMMENT_ID>`
  Report: "Comment added to PROJ-123 -- [View comment](<URL>)"

---

## Guardrails

- **Never display or log the API token**
- **Confirm before executing** -- always tell the user what you're about to change and get confirmation before making the API call
- **Prompt for missing inputs** -- if ticket key or comment text is not provided, use `AskUserQuestion` to request the missing information before proceeding
- **No credential prompting** -- if env vars are missing, instruct the user to set them
- **Parse JSON directly** -- do not rely on jq
- **One operation at a time** -- if the user wants multiple changes, execute them sequentially and confirm each

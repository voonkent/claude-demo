---
name: jira-fetch-ticket
description: Fetch Jira ticket details or search tickets by JQL. Use when the user wants to look up a Jira issue, check ticket status, or search for tickets.
---

Fetch a Jira Cloud ticket by key or search tickets via JQL query.

**Input**: A ticket key (e.g., `PROJ-123`) or a JQL query string. May be provided as an argument or extracted from conversation context.

**Note**: Environment variables (`JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_BASE_URL`) are validated by the orchestrating agent before this skill is invoked.

---

## Steps

### Step 1 -- Determine operation type

- If the input matches `[A-Z][A-Z0-9]+-[0-9]+` (e.g., `PROJ-123`), this is a **single ticket fetch**.
- If the input looks like a JQL query (e.g., `project = PROJ AND status = Open`), this is a **search**.
- If no input was provided, ask the user with AskUserQuestion.

### Step 2a -- Fetch single ticket

```bash
curl -s -w "\n%{http_code}" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -H "Accept: application/json" \
  "${JIRA_BASE_URL}/rest/api/3/issue/<TICKET_KEY>?expand=renderedFields&fields=summary,status,assignee,reporter,priority,description,comment,labels,customfield_10020"
```

Replace `<TICKET_KEY>` with the actual ticket key.

### Step 2b -- Search tickets (JQL)

```bash
curl -s -w "\n%{http_code}" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -H "Accept: application/json" \
  --data-urlencode "jql=<JQL_QUERY>" \
  "${JIRA_BASE_URL}/rest/api/3/search?fields=summary,status,assignee,priority&maxResults=20"
```

### Step 3 -- Handle errors

Split the curl output: everything except the last line is JSON, the last line is the HTTP status code.

| Status | Action |
|--------|--------|
| `200` | Success -- proceed to formatting |
| `401` | Auth failed -- tell user to verify `JIRA_EMAIL` and `JIRA_API_TOKEN` |
| `403` | Permission denied -- user may lack access to this project |
| `404` | Ticket not found -- verify the key is correct |
| Other | Report the status code and response body |

### Step 4 -- Format and display

**For single ticket**, present as:
```
## <KEY>: <summary>

| Field     | Value          |
|-----------|----------------|
| Status    | <status>       |
| Priority  | <priority>     |
| Assignee  | <displayName or "Unassigned"> |
| Reporter  | <displayName or "Unknown">    |
| Labels    | <comma-separated or "None">   |
| Sprint    | <sprint name or "None">       |

### Description
<description converted to markdown>

### Comments (N)
**<author displayName>** -- <created date>
<comment body>
```

**For search results**, present as:
```
## Search Results (N tickets)

| Key | Summary | Status | Assignee | Priority |
|-----|---------|--------|----------|----------|
```

**Formatting rules:**
- Prefer `renderedFields.description` (HTML) over `fields.description` (ADF JSON). Convert HTML to readable markdown.
- For assignee/reporter: use `displayName`. Show "Unassigned" / "Unknown" if null.
- For sprint: check `fields.customfield_10020` -- typically an array of sprint objects. Show the name of the last (most recent) entry. Show "None" if empty/null.
- For comments: show the **5 most recent** (latest first). If more exist, note "Showing 5 of N comments".
- Format dates as `YYYY-MM-DD HH:MM`.

---

## Guardrails

- **Never display or log the API token**
- **Read-only** -- this skill only fetches data, never creates or modifies tickets
- **No credential prompting** -- if env vars are missing, instruct the user to set them
- **Parse JSON directly** -- do not rely on jq

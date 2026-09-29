---
name: crm-e2e
description: Verify critical CRM user flows in a real browser. Use after changes affecting navigation, forms, authentication, pipeline interactions or multi-step CRM workflows.
---

Use Playwright MCP to verify the affected browser flow.

Prefer realistic user interaction over implementation-specific checks.

Critical workflow:

Login
→ create Lead
→ qualify Lead
→ convert Lead to Client
→ create Opportunity
→ move Opportunity through Pipeline
→ create Activity
→ mark Opportunity Won

Verify:

- navigation
- validation errors
- successful persistence
- visible status changes
- authorization behavior
- redirects
- user feedback

Do not modify application data unrelated to the test.
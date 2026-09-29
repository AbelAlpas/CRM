---
name: thymeleaf-ui
description: Build or modify CRM pages using Thymeleaf and Bootstrap. Use when creating forms, tables, dashboards, navigation, modals or pipeline UI.
---

Follow the visual conventions already present in the CRM.

Use:

- Thymeleaf
- Bootstrap
- Bootstrap Icons

Prefer Bootstrap utilities before custom CSS.

Reuse fragments for:

- navbar
- sidebar
- alerts
- forms
- pagination

Forms must:

- use proper labels
- preserve submitted values after validation errors
- display field-level validation errors
- display global errors when applicable

Tables should support empty states.

Use responsive Bootstrap components.

Avoid inline CSS.

Avoid inline JavaScript when a reusable file is appropriate.

Keep JavaScript minimal.

For destructive actions such as delete, require confirmation.

When modifying important screens, verify the result using Playwright MCP.
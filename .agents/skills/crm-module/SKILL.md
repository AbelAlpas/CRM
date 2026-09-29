---
name: crm-module
description: Implement or modify a CRM business module end-to-end. Use when adding or changing Leads, Clients, Opportunities, Activities, Pipeline, Dashboard or Users.
---

Implement the requested CRM feature following the project's modular
Spring Boot architecture.

Before changing code:

1. Inspect the existing module and nearby modules for established patterns.
2. Read docs/business-rules.md only when business behavior is involved.
3. Read docs/database.md only when persistence changes are involved.

For a typical feature, consider whether it requires:

- Entity changes
- DTO/form object
- Repository
- Service
- Controller
- Thymeleaf templates
- Validation
- Security rules
- Tests

Do not create layers that the feature does not need.

Business logic belongs in services.

Controllers coordinate HTTP requests and responses.

Repositories handle persistence.

Keep Thymeleaf templates presentation-focused.

If the database schema changes, use the postgres-migration skill.

If the UI is created or substantially modified, use the thymeleaf-ui skill.

After implementation:

- compile the project
- run relevant tests
- verify affected browser flows when practical
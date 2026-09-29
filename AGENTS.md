# CRM Project Instructions

## Project

This repository contains a server-rendered CRM application.

The CRM manages:

- Dashboard
- Leads
- Clients
- Opportunities
- Sales Pipeline
- Activities
- Users and permissions

Keep the application as a modular monolith.

Do not introduce microservices, React, Angular, Vue, Node.js, or a separate frontend unless explicitly requested.

---

## Technology stack

Backend:

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Spring Security
- Jakarta Bean Validation

Frontend:

- Thymeleaf
- Bootstrap 5
- Bootstrap Icons
- Vanilla JavaScript only when required

Database:

- PostgreSQL
- Flyway for schema migrations

Testing:

- JUnit
- Mockito
- MockMvc
- Testcontainers when integration tests require PostgreSQL

Build:

- Maven

Always respect the dependency versions already defined in pom.xml.

Do not upgrade dependencies unless explicitly requested.

When framework or library behavior is uncertain or version-dependent,
consult Context7 documentation before implementing.

---

## Architecture

Use a modular monolith organized primarily by business feature.

Main modules:

- dashboard
- lead
- client
- opportunity
- pipeline
- activity
- user
- security
- config

Each business module may contain:

- entity
- controller
- service
- repository
- dto
- mapper when required

Controllers must not contain business logic.

Business rules belong in services.

Database access belongs in repositories.

Do not access repositories directly from controllers.

Avoid unnecessary abstractions.

Do not create interfaces with only one implementation unless they provide
real architectural value.

---

## MVC conventions

Use Spring MVC controllers for web pages.

Controllers return Thymeleaf templates.

Use Post/Redirect/Get after successful form submissions.

Example:

GET /leads
GET /leads/new
POST /leads
GET /leads/{id}
GET /leads/{id}/edit
POST /leads/{id}/edit
POST /leads/{id}/delete

Avoid REST controllers unless an endpoint is genuinely needed by
JavaScript or an external integration.

---

## Thymeleaf conventions

Reusable UI must use Thymeleaf fragments.

Shared fragments include:

- sidebar
- navbar
- layout
- alerts
- pagination
- form errors

Templates must not contain business logic.

Use Bootstrap classes before writing custom CSS.

Avoid inline CSS.

Avoid duplicated markup.

All forms must show validation errors.

---

## Database

PostgreSQL is the only production database.

Use Flyway for every schema change.

Never rely on Hibernate automatic schema creation for production.

Do not change an existing applied Flyway migration.

Create a new migration instead.

Migration naming:

V1__initial_schema.sql
V2__create_leads.sql
V3__create_clients.sql

Use snake_case database identifiers.

Use explicit foreign keys.

Use indexes for frequently queried foreign keys and filtering columns.

Never delete production data or execute destructive migrations unless
explicitly requested.

Refer to docs/database.md when changing the schema.

---

## CRM business rules

A Lead represents a prospect who has not yet become a client.

A Lead may be:

- NEW
- CONTACTED
- QUALIFIED
- DISQUALIFIED
- CONVERTED

Converting a Lead creates a Client while preserving the original Lead
for traceability.

A Client represents a person or company with an established commercial
relationship.

An Opportunity represents a potential sale.

Every Opportunity must have:

- client
- owner
- pipeline stage
- expected amount
- status
- created date

Pipeline stages determine the visual position of an Opportunity.

Example pipeline:

PROSPECTING
QUALIFICATION
PROPOSAL
NEGOTIATION
WON
LOST

Moving an opportunity between stages must persist the new stage.

Activities may belong to:

- Lead
- Client
- Opportunity

Activity types:

- TASK
- CALL
- MEETING
- FOLLOW_UP

Activities may have:

- owner
- due date
- status
- notes

---

## Security

Use Spring Security.

Never implement custom password hashing.

Passwords must be stored using BCrypt or the configured Spring
PasswordEncoder.

Never expose passwords in DTOs, logs, templates or responses.

Initial application roles:

ADMIN
MANAGER
SALES

ADMIN:
full system access.

MANAGER:
access to commercial information and reports.

SALES:
access to assigned commercial operations.

Authorization must be enforced server-side.

Hiding a button in Thymeleaf is not authorization.

---

## Validation

Validate input on the server using Jakarta Bean Validation.

Never trust browser validation alone.

Use DTOs for forms when binding entities directly would expose fields
that users should not modify.

Validate email, required values, amounts and dates.

---

## Error handling

Provide meaningful validation errors to users.

Do not expose stack traces or database errors in the UI.

Log technical details server-side.

Use a global exception handler when appropriate.

---

## Testing

Tests should focus on business behavior.

When modifying a service, run its relevant unit tests.

When modifying controllers, run relevant MVC tests.

When modifying database behavior, prefer integration tests using
PostgreSQL/Testcontainers when practical.

Use Playwright for critical end-to-end flows.

Critical CRM flow:

Lead
→ qualification
→ conversion to Client
→ Opportunity
→ Pipeline movement
→ Won/Lost

---

## Definition of done

A feature is complete when:

- the project compiles
- relevant tests pass
- validation works
- authorization is enforced
- persistence works
- Thymeleaf pages render correctly
- forms display errors correctly
- no unrelated behavior has been changed

When the change affects the UI, verify the relevant flow in the browser.

---

## Project documentation

There is not documentation yet
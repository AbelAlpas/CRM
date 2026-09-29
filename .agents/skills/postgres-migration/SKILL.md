---
name: postgres-migration
description: Create and validate PostgreSQL Flyway migrations. Use when adding or changing tables, columns, indexes, constraints or database relationships.
---

Inspect the current Flyway migration history before creating a migration.

Never modify an already applied migration.

Create the next sequential migration under:

src/main/resources/db/migration/

Use PostgreSQL-compatible SQL.

Database conventions:

- snake_case identifiers
- explicit primary keys
- explicit foreign keys
- NOT NULL where logically required
- appropriate unique constraints
- indexes for common filtering and foreign keys

Consider referential integrity before adding DELETE behavior.

Do not use destructive operations unless explicitly requested.

After changing the schema:

1. update affected JPA entities
2. update repositories if necessary
3. update docs/database.md when the schema model materially changes
4. run relevant integration tests
# Flyway database migrations

The backend now treats Flyway as the owner of database schema changes. Hibernate validates the schema and no longer mutates it automatically.

## Runtime order

1. Flyway reads `db/migration/{vendor}`.
2. Pending migrations run in version order.
3. Hibernate validates the mapped entities against the migrated schema.
4. `CategorySeeder` inserts or updates application category data.
5. The API starts.

`{vendor}` resolves to `h2` locally and `postgresql` on Neon/Render.

## Existing Neon database: first deployment

The current database already contains application tables but does not yet contain `flyway_schema_history`. Keep this setting for the first deployment:

```text
FLYWAY_BASELINE_ON_MIGRATE=true
```

Flyway records the existing schema as baseline version `1`, then applies `V2`, `V3`, and later migrations. After the first successful deployment and after `flyway_schema_history` exists, change the Render variable to:

```text
FLYWAY_BASELINE_ON_MIGRATE=false
```

This restores Flyway's safety check against connecting to an unexpected non-empty database.

## Creating the next migration

Never edit a versioned migration that has already run in any environment. Add a new script to both vendor folders:

```text
backend/src/main/resources/db/migration/postgresql/V5__add_example_column.sql
backend/src/main/resources/db/migration/h2/V5__add_example_column.sql
```

Example:

```sql
alter table pmp_questions
    add column review_priority integer;
```

Use the same version and description for H2 and PostgreSQL. Vendor-specific syntax can differ inside the files.

## Verification

From `backend/`:

```bash
mvn test
mvn spring-boot:run
```

Inspect migration history:

```sql
select installed_rank, version, description, type, installed_on, success
from flyway_schema_history
order by installed_rank;
```

## Failure rules

- Do not delete rows from `flyway_schema_history` manually.
- Do not rename or modify an applied `V...sql` file.
- Fix a migration with a new higher version.
- Keep `spring.flyway.clean-disabled=true` in production.
- Back up important data before destructive migrations.

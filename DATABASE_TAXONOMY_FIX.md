# PostgreSQL category taxonomy constraint fix

## Symptom

```text
ERROR: new row for relation "categories" violates check constraint "categories_taxonomy_check"
```

## Cause

The database was created before `PMP_TOPIC` was added. The old PostgreSQL CHECK constraint still accepted only the legacy taxonomy values.

## Flyway fix

The fix is now versioned as:

```text
backend/src/main/resources/db/migration/postgresql/V3__expand_category_taxonomy.sql
```

On the first deployment to an existing Neon database, Flyway baselines the old schema at version 1 and automatically applies V2, V3, and later migrations before Hibernate validation and category seeding.

Do not run the old Java startup migration. `CategoryTaxonomyConstraintMigration` has been removed.

See `docs/FLYWAY_MIGRATIONS.md` for the normal workflow.

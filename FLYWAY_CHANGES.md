# Flyway integration changes

- Added `flyway-core` and the PostgreSQL Flyway database module.
- Added vendor-specific migrations for PostgreSQL/Neon and H2.
- Added a version-1 schema for new databases.
- Added upgrade migrations for explanation review columns, category taxonomy, and indexes.
- Replaced Hibernate schema mutation with `ddl-auto=validate`.
- Removed `CategoryTaxonomyConstraintMigration`; Flyway V3 now owns that change.
- Added a migration integration test using H2.
- Added first-deployment baseline configuration for the existing Neon database.
- Added migration workflow documentation in `docs/FLYWAY_MIGRATIONS.md`.

# Error handling and unified PMP categories

## Error flow

1. Backend services throw typed exceptions or validation exceptions.
2. `ApiExceptionHandler` returns a stable JSON contract with `code`, `message`, `fieldErrors`, `details`, `path`, and `traceId`.
3. Angular `apiErrorInterceptor` catches all failed HTTP calls.
4. `ErrorModalService` opens the global modal while the original observable error is rethrown so pages can stop loading indicators.

## Active category taxonomy

The active taxonomy is `PMP_TOPIC`. Legacy PMBOK/PMA category rows remain in the database but are set inactive for migration safety.

Use `POST /api/questions/reclassify` after deployment to classify all existing questions into the new topics.

## Existing Neon database migration

Older database versions may contain a PostgreSQL enum-style CHECK constraint that only accepts
`PMBOK8_DOMAIN` and `PMA_HANDOUT_TOPIC`. Hibernate `ddl-auto=update` does not automatically widen
that constraint when `PMP_TOPIC` is introduced.

The application now repairs `categories_taxonomy_check` before seeding categories. For an immediate
manual fix, run `docs/migration-fix-category-taxonomy.sql` in the Neon SQL Editor, then redeploy.

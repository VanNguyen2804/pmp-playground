# PostgreSQL `lower(bytea)` fix

## Symptom

Calling `GET /api/questions` returns HTTP 500 and PostgreSQL reports:

```text
ERROR: function lower(bytea) does not exist
SQLState: 42883
```

## Causes fixed

1. Older application versions used LOB mappings for Java `String` fields. Existing Neon columns could therefore be `bytea` or `oid` instead of PostgreSQL `text`.
2. The former JPQL query always contained `lower(...)` expressions, even when `search`, `categoryCode`, and other filters were empty.

## Changes

- `V5__convert_legacy_binary_text_columns.sql` converts legacy binary text columns to `text`.
- `QuestionRepository` now supports `JpaSpecificationExecutor`.
- `QuestionSpecifications` adds predicates only for filters that are actually supplied.
- `ApiExceptionHandler` returns `DATABASE_QUERY_FAILED` with `SQLState` and `X-Trace-Id`.
- Angular reads structured errors from JSON objects, JSON strings, or Blob bodies and displays them in the global modal.

## Confirm Flyway ran

```sql
select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;
```

Expected latest migration:

```text
5 | 5 | convert legacy binary text columns | true
```

## Confirm PostgreSQL types

```sql
select table_name, column_name, data_type, udt_name
from information_schema.columns
where table_schema = current_schema()
  and (
    (table_name = 'pmp_questions' and column_name in (
      'question_text', 'pma_explanation', 'ai_explanation',
      'final_explanation', 'explanation_review_notes', 'tags'
    ))
    or (table_name = 'question_options' and column_name = 'option_text')
    or (table_name = 'matching_pairs' and column_name in ('left_text', 'right_text'))
  )
order by table_name, column_name;
```

All listed columns should have `data_type = text`.

## Expected API error format

When a later database query fails, the backend returns JSON instead of an HTML error page:

```json
{
  "status": 500,
  "code": "DATABASE_QUERY_FAILED",
  "message": "Không thể đọc dữ liệu từ database. Vui lòng thử lại sau khi migration hoàn tất.",
  "path": "/api/questions",
  "details": ["SQLState: 42883"],
  "traceId": "a1b2c3d4"
}
```

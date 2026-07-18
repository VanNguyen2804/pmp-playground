# Lower bytea and frontend error-display changes

- Added Flyway PostgreSQL migration V5 to convert legacy `bytea`/`oid` String columns to `text`.
- Added matching H2 V5 no-op migration so migration versions remain aligned.
- Replaced nullable static JPQL search with conditional JPA Specifications.
- Added structured handling for all Spring `DataAccessException` errors.
- Added `X-Trace-Id` response header and exposed it through CORS.
- Improved Angular error parsing for JSON objects, JSON strings, and Blob responses.
- Added backend tests for structured database errors and conditional question search.
- Frontend `npm ci` and production build verified successfully.

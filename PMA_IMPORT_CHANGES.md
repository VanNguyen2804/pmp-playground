# PMA import changes

## Backend

- Redesigned question persistence for MCQ, MRQ and matching questions.
- Added idempotent PMA exam importer for `exam_attempt.exam_content.questions`.
- Added normalized option and matching-pair tables.
- Added PMA explanation fields and `NOT_PROVIDED` status when explanation text is absent.
- Added PMBOK 8 and PMA Handout category taxonomies with automatic initial classification.
- Added category counts and filtering APIs.
- Preserved PMA source objects in `raw_source_json` for traceability.

## Frontend

- Added **Categories** page.
- Added PMBOK 8/PMA category filters.
- Added PMA JSON upload summary with inserted/updated/skipped counts.
- Added MCQ, MRQ and matching display/practice support.
- Added manual explanation editing for MCQ/MRQ.

## Data

- Added `sample-data/exam1.json`.
- Confirmed 180 questions: 172 MCQ, 4 MRQ and 4 matching.

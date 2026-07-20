# Question image overrides

This update adds local frontend-hosted images for two PMA questions so they no longer depend on missing or expiring external image URLs.

## Updated questions

- `PMP Full Test 01 - 0162`
  - Image: `/question-images/pmp-full-test-01-0162.png`
- `PMP Full Test 01 - 0175`
  - Image: `/question-images/pmp-full-test-01-0175.svg`

## Implementation

- Frontend assets were added under `frontend/public/question-images/`.
- Backend adds a startup seeder `QuestionImageOverrideSeeder` that updates these two questions by `examName`.
- `QuestionRepository` now includes `findByExamNameIgnoreCase(...)`.

This is idempotent and safe to rerun.

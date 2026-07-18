# Explanation comparison workflow

The question model now stores three separate explanation fields:

1. `pma_explanation` — original PMA explanation.
2. `ai_explanation` — independent ChatGPT/AI explanation.
3. `final_explanation` — reviewed explanation used in practice mode.

Additional review metadata:

- `final_explanation_source`: `NONE`, `PMA`, `AI`, `MERGED`, or `MANUAL`.
- `explanation_review_status`: `PENDING` or `REVIEWED`.
- `explanation_review_notes`: why one explanation was selected or how they were combined.
- `explanation_reviewed_at`: automatically set when a record is saved as reviewed.

## UI workflow

Open a question and select **So sánh / Sửa**:

- Edit or paste the PMA explanation.
- Paste the ChatGPT explanation.
- Use **Dùng lời giải PMA**, **Dùng lời giải AI**, or **Ghép PMA + AI**.
- Edit the final explanation as needed.
- Set the review status to **Đã duyệt và chốt**.

Practice mode displays the final explanation first. If no final explanation exists, it falls back to AI, then PMA.

## Existing PostgreSQL database

Run `docs/migration-add-explanation-review.sql` before redeploying, or allow Hibernate `ddl-auto=update` to add the columns.

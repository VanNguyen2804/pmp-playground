# Database design

The question bank uses a normalized schema that works with H2 locally and PostgreSQL/Neon in production.

```text
categories
  1 ───────< question_categories >────── 1 pmp_questions
                                                    │
                                                    ├────< question_options
                                                    │
                                                    └────< matching_pairs
```

## Tables

### `pmp_questions`

Stores the common question data.

| Column | Purpose |
|---|---|
| `id` | Internal database identifier |
| `external_id` | Stable PMA UUID used for idempotent re-import/upsert |
| `exam_name` | Example: `PMP Full Test 01 - 0009` |
| `question_type` | `MCQ`, `MRQ`, or `MATCHING` |
| `question_text` | Full question text |
| `image_url` | Optional image URL from the source JSON |
| `pma_explanation` | Explanation from PMA, imported or entered manually |
| `ai_explanation` | Independent explanation produced by ChatGPT/AI |
| `final_explanation` | Curated explanation selected for study and practice |
| `final_explanation_source` | `NONE`, `PMA`, `AI`, `MERGED`, or `MANUAL` |
| `explanation_review_status` | `PENDING` or `REVIEWED` |
| `explanation_review_notes` | Notes comparing PMA and AI reasoning |
| `explanation_reviewed_at` | Timestamp when the explanation was finalized |
| `explanation_status` | PMA-source status: `NOT_PROVIDED`, `IMPORTED`, or `MANUAL` |
| `explanation_type` | Source metadata such as `ai` |
| `explanation_prompt_version` | Source prompt version metadata |
| `number_of_answers` | Expected number of answers or matching pairs |
| `source` | `PMA`, `MANUAL`, `CSV`, or `JSON` |
| `difficulty`, `reference`, `tags` | Optional learning metadata |
| `raw_source_json` | Original question object for traceability |
| `created_at`, `updated_at` | Audit timestamps |

### `question_options`

One row per option. `correct=true` supports both one-answer MCQ and multi-answer MRQ.

| Column | Purpose |
|---|---|
| `question_id` | Parent question |
| `option_key` | A, B, C, D, E... |
| `option_text` | Option content |
| `display_order` | Stable ordering |
| `correct` | Whether the option is correct |

### `matching_pairs`

Stores the correct left/right mapping for matching questions.

### `categories`

Stores two taxonomies:

- `PMBOK8_DOMAIN`: Governance, Scope & Quality, Schedule, Finance, Stakeholders, Resources, Risk.
- `PMA_HANDOUT_TOPIC`: Integration & Change, Scope & Requirements, Schedule, Cost & EVM, Quality, Resources/Team/Leadership, Communications/Stakeholders, Risk, Procurement, Agile/Hybrid, Business Environment/Compliance, Closing/Knowledge.

### `question_categories`

Many-to-many relationship. A question can appear under both a PMBOK 8 domain and one or more PMA Handout topics.

## Import behavior

- The backend detects `exam_attempt.exam_content.questions` automatically.
- `external_id` makes import idempotent: importing the same file again updates existing questions instead of duplicating them.
- MCQ, MRQ, matching pairs, image URL, answer metadata, and raw source JSON are preserved.
- Categories are initially assigned by keyword rules and may be edited manually.
- The supplied `exam1.json` contains explanation metadata but does not contain explanation text. Those records are saved with `explanation_status=NOT_PROVIDED`.
- Re-importing PMA data does not erase manually curated AI or final explanations.
- The edit page presents PMA, AI, and final explanations side by side. The final explanation may be copied from either source, merged, or manually rewritten.

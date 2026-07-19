# AI and Long Question practice bank

## Categories

- `TOPIC_AI` — practical AI use cases, human oversight, privacy, bias, reliability, accountability, prompting, automation, assistance, and augmentation.
- `TOPIC_LONG_QUESTION` — long case-based questions, including two shared cases with six questions each.

## Curated questions

- 12 standalone AI scenario questions.
- 6 long questions sharing an AI-enabled public-health platform case.
- 6 long questions sharing a global ERP transformation case.
- Total new questions: 24.
- AI questions: 18.
- Long questions: 12.

Stable external IDs make the seeder idempotent and preserve answer history:

- `AI-PRACTICE-0001` ... `AI-PRACTICE-0012`
- `LONG-CASE-AI-0001` ... `LONG-CASE-AI-0006`
- `LONG-CASE-GENERAL-0001` ... `LONG-CASE-GENERAL-0006`

Existing imported questions are also classified automatically when:

- their content explicitly references AI concepts; or
- the question stem is at least 700 characters long.

No database migration is required because the existing category and question tables support these additions.

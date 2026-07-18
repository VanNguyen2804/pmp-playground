# PMP Playground

A full-stack PMP question bank built with Angular, Java 21/Spring Boot, and H2/PostgreSQL. The backend imports the PMA exam-export JSON shape and stores MCQ, MRQ, matching questions, correct answers, PMA explanation metadata, and learning categories.

## Stack

- Frontend: Angular 21 standalone components
- Backend: Java 21, Spring Boot 3.5, Spring Data JPA
- Local database: H2 file database
- Production database: PostgreSQL/Neon
- Deployment: Render Blueprint (`render.yaml`)
- CI: GitHub Actions

## New PMA import support

Upload `sample-data/exam1.json` from the **Upload PMA** page. The backend detects:

```text
exam_attempt.exam_content.questions
```

The supplied file contains:

- 180 questions
- 172 MCQ
- 4 MRQ
- 4 matching questions
- PMA UUIDs, exam names, options, correct answers, image URLs, and explanation metadata

The file does **not** contain the actual explanation text; it contains values such as `explanation_type=ai` and `explanation_prompt_version`. The backend therefore saves `pma_explanation` as null and sets `explanation_status=NOT_PROVIDED`. Explanations can be added later from the edit form or a future export that contains `explanation`, `explanation_text`, `solution`, or `rationale`.

## Explanation comparison workflow

Each question now keeps three independent columns:

- `pma_explanation` — explanation from PMA.
- `ai_explanation` — explanation from ChatGPT/AI.
- `final_explanation` — reviewed explanation used for learning.

The edit screen shows the three columns side by side and provides buttons to choose PMA, choose AI, or merge both. Review metadata records the selected source, review status, notes, and review timestamp. Practice mode prefers the final explanation, then falls back to AI and PMA. See [EXPLANATION_REVIEW_CHANGES.md](EXPLANATION_REVIEW_CHANGES.md).

Some question images use temporary signed URLs. The URL is preserved, but it may expire. A later enhancement can add permanent image upload/storage.

## Category taxonomy

Questions use a many-to-many category model. The importer applies initial keyword-based classification; users can correct categories manually.

### PMBOK 8 domains

- Governance
- Scope & Quality
- Schedule
- Finance
- Stakeholders
- Resources
- Risk

### PMA Handout topics

- Integration & Change
- Scope & Requirements
- Schedule
- Cost & EVM
- Quality
- Resources, Team & Leadership
- Communications & Stakeholders
- Risk
- Procurement
- Agile & Hybrid
- Business Environment & Compliance
- Closing & Knowledge

See [DATABASE_DESIGN.md](DATABASE_DESIGN.md) for the ERD and table details.

## Repository structure

```text
pmp-playground/
├── frontend/
├── backend/
├── sample-data/
│   └── exam1.json
├── DATABASE_DESIGN.md
├── render.yaml
└── .github/workflows/ci.yml
```

## Local development

### Backend

Requirements: Java 21 and Maven.

```bash
cd backend
mvn spring-boot:run
```

- API root: `http://localhost:8080/api`
- H2 console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/pmpdb`
- Username: `sa`
- Password: empty

An older release used a different `questions` table. The redesigned version uses `pmp_questions`. For a clean local database, stop the backend and remove `backend/data/` before restarting. For an existing PostgreSQL/Neon database, run `docs/migration-add-explanation-review.sql` before redeploying.

### Frontend

Requirements: Node.js 22 and npm.

```bash
cd frontend
npm ci
npm start
```

Open `http://localhost:4200`. Angular provides hot reload locally.

## Import the PMA file

### Through the UI

1. Start backend and frontend.
2. Open **Upload PMA**.
3. Select `sample-data/exam1.json`.
4. Click **Upload and save database**.
5. Open **Categories** or **Questions** to browse the imported data.

### Through the API

```bash
curl -X POST http://localhost:8080/api/questions/import/pma-exam \
  -F "file=@sample-data/exam1.json"
```

The import is idempotent. PMA `id` is stored as `external_id`; importing the same export again updates the existing records rather than creating duplicates.

## API summary

### Questions

- `GET /api/questions`
  - filters: `search`, `categoryCode`, `taxonomy`, `difficulty`, `questionType`, `reviewStatus`, `page`, `size`
- `GET /api/questions/{id}`
- `POST /api/questions`
- `PUT /api/questions/{id}`
- `PUT /api/questions/{id}/explanations` — update PMA, AI, final explanation, and review metadata without modifying the question
- `DELETE /api/questions/{id}`
- `GET /api/questions/random?count=10&categoryCode=PMA_AGILE_HYBRID`

### Imports

- `POST /api/questions/import/pma-exam`
- `POST /api/questions/import/json` — auto-detects PMA export or generic request array
- `POST /api/questions/import/csv`

### Categories

- `GET /api/categories`
- `GET /api/categories?taxonomy=PMBOK8_DOMAIN`
- `GET /api/categories?taxonomy=PMA_HANDOUT_TOPIC`

## Generic question JSON

```json
{
  "questionType": "MRQ",
  "questionText": "Choose two appropriate actions.",
  "options": [
    {"key": "A", "text": "Action A"},
    {"key": "B", "text": "Action B"},
    {"key": "C", "text": "Action C"},
    {"key": "D", "text": "Action D"}
  ],
  "correctAnswers": ["A", "C"],
  "matchingPairs": [],
  "pmaExplanation": "PMA explanation text",
  "aiExplanation": "ChatGPT explanation text",
  "finalExplanation": "Reviewed final explanation",
  "finalExplanationSource": "MERGED",
  "explanationReviewStatus": "REVIEWED",
  "explanationReviewNotes": "PMA answer retained; AI reasoning made clearer.",
  "source": "MANUAL",
  "categoryCodes": ["P8_RESOURCES", "PMA_RESOURCE_TEAM"]
}
```

## Production deployment on Render + Neon

1. Create a Neon PostgreSQL database.
2. Push the repository to GitHub.
3. In Render, select **New → Blueprint** and connect the repository.
4. Enter:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
5. Render creates:
   - `pmp-playground-api`
   - `pmp-playground-web`

Frontend publish directory, relative to `frontend`, is:

```text
dist/frontend/browser
```

## Verification performed

- Angular production build completed successfully.
- `exam1.json` was parsed locally and contains the counts listed above.
- Maven dependencies could not be downloaded in the artifact environment because Maven Central DNS was unavailable. GitHub Actions or Render will perform the Spring Boot build with network access.

## Security

Never commit database passwords, GitHub tokens, or production secrets. Use Render environment variables.

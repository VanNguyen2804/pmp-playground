# Practice answer history

## Home page

The Angular root route (`/`) now redirects to `/practice`. Clean URL routing remains enabled, so the Render static-site rewrite must keep sending `/*` to `/index.html`.

## Database migration

Flyway migration `V6__create_question_answer_history.sql` creates `question_answer_attempts` for PostgreSQL and H2.

Each row stores one submitted answer, whether it was correct, the question type, a practice-session ID, and the answer time. Deleting a question deletes its history through `ON DELETE CASCADE`.

## API

Submit and record an attempt:

```http
POST /api/questions/{questionId}/attempts
Content-Type: application/json
```

MCQ/MRQ request:

```json
{
  "selectedAnswers": ["B"],
  "matchingAnswers": {},
  "sessionId": "practice-session-id"
}
```

Matching request:

```json
{
  "selectedAnswers": [],
  "matchingAnswers": {
    "Risk": "An uncertain event",
    "Issue": "An event that has occurred"
  },
  "sessionId": "practice-session-id"
}
```

The backend evaluates correctness; it does not trust a correctness flag from the browser.

Read history:

```http
GET /api/questions/{questionId}/attempts?page=0&size=20
GET /api/questions/{questionId}/attempts/summary
```

The summary returns total, correct and incorrect attempt counts, accuracy percentage, and the latest result.

# Highlight & Study Notes

## User experience

- Select text in the question or an answer option.
- Choose yellow, green, blue, or pink.
- Click an existing highlight to change its color or delete it.
- Open **Ghi chú** to view, edit, or delete the note saved for that question.
- Highlight changes are optimistically rendered and automatically saved with a short debounce.
- Notes and highlights are restored after reload.

## Performance design

- The frontend renders highlights as safe Angular text segments; it does not use `innerHTML`.
- Annotations are loaded in one batch request for the current question set instead of one request per question.
- A local Signals cache keeps annotations available while navigating between questions.
- Highlight writes are debounced and serialized per question to avoid unnecessary requests and stale responses.

## API

```http
POST /api/questions/study-annotations/batch
GET  /api/questions/{id}/study-annotation
PUT  /api/questions/{id}/study-annotation
```

Example PUT body:

```json
{
  "note": "FIRST means assess before acting.",
  "highlights": [
    {
      "id": "4f93c49e-5c2b-46de-b061-c8e46306019c",
      "target": "QUESTION",
      "targetKey": null,
      "startOffset": 12,
      "endOffset": 28,
      "text": "What should ...",
      "color": "YELLOW"
    }
  ]
}
```

## Database

Flyway V7 creates `question_study_annotations` with one row per question:

- `question_id` unique FK
- `note_text`
- `highlights_json`
- optimistic `version`
- `created_at`, `updated_at`

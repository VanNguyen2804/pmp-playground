# Session report and PMBOK 8 AI question changes

## Practice completion report

After a practice set finishes, the frontend requests a backend-generated report for that exact session:

```http
POST /api/questions/practice/session-report
```

Request:

```json
{
  "sessionId": "practice-...",
  "questionIds": [1, 2, 3]
}
```

The response contains:

- answered, correct, incorrect, skipped and accuracy totals;
- wrong-answer counts by knowledge category;
- up to five category-specific study suggestions;
- PMBOK 8 sections, focus areas, decision rules and recommended practice.

`Long Question` and `Chart` are treated as presentation/format categories and are excluded from the study recommendation ranking. `AI` remains a knowledge category.

A new Flyway V8 migration indexes `(session_id, answered_at)` for fast session report retrieval.

## PMBOK 8 AI question bank

The seeder adds 18 scenario-based questions with stable IDs:

```text
AI-PMBOK8-0001 ... AI-PMBOK8-0018
```

The questions cover Appendix X3 topics including:

- real-time monitoring and early warning signals;
- baseline optimization;
- AI chatbots, task support and meeting minutes;
- risk identification and automated mitigation controls;
- privacy, bias, accountability, reliability, transparency and safety;
- copyright, sustainability and provider data handling;
- Automation, Assistance and Augmentation.

Rerunning the backend updates the curated questions without creating duplicates or deleting answer history.

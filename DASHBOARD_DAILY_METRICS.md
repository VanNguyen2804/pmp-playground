# Dashboard metrics: today, daily comparison, and question-bank coverage

The practice dashboard now uses the browser's IANA time zone when requesting metrics:

```http
GET /api/questions/practice/dashboard?timeZone=Asia/Ho_Chi_Minh
```

The response contains:

- today's accuracy and attempt totals;
- yesterday's accuracy and attempt totals;
- the accuracy difference in percentage points and a status (`IMPROVING`, `STABLE`, `DECLINING`, `NEW_BASELINE`, or `NO_DATA`);
- the number of unique questions answered at least once;
- the total number of questions currently stored in the question bank;
- question-bank coverage percentage;
- existing wrong-question and correct-streak metrics.

`answeredQuestions` counts unique question IDs with at least one saved answer attempt. It does not count repeated attempts as additional questions.

The comparison thresholds are shared with analytics:

- improvement of at least 3 percentage points: `IMPROVING`;
- decline of at least 3 percentage points: `DECLINING`;
- otherwise: `STABLE`;
- no attempts yesterday: `NEW_BASELINE`;
- no attempts today: `NO_DATA`.

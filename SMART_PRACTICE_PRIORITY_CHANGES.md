# Smart practice priority

The `/exam` page now loads its normal question set from:

`GET /api/questions/practice/priority`

Selection uses weighted random sampling without replacement:

- latest answer is wrong: highest priority;
- never answered: high priority;
- still learning (fewer than 5 correct answers): normal rotation;
- latest answer is correct and total correct answers are 5 or more: low frequency;
- a newly wrong answer immediately restores high priority.

The existing `GET /api/questions/random` endpoint remains unchanged for callers that need uniform random selection.

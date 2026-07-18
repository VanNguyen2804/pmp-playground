# Practice Review UI and Backend

## Frontend
- Rebuilt the application shell with a sidebar, sticky search header, profile area, KPI cards, filters, question panel, explanation panel, and answer-history table.
- Practice is the home page.
- Added wrong-question review filters: latest answer wrong, minimum incorrect count, category, and shuffle.
- Added Angular Signal state for dashboard, filters, quiz, history, and explanation editor.
- Added inline editing of PMA, AI, and final explanations. A manual explanation can be saved and promoted to the reviewed final explanation.

## Backend
- `GET /api/questions/practice/dashboard`
- `GET /api/questions/review/wrong?categoryCode=&minIncorrect=1&count=50&shuffle=true`
- Reuses existing answer-attempt history and explanation review endpoint.

The wrong-question queue considers the latest attempt for each question. A question leaves the queue after its latest attempt is correct and returns if answered incorrectly later.

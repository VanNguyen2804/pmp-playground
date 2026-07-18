# Answer history changes

- The frontend home page now opens **Luyện tập**.
- Every click on **Kiểm tra** submits the selected answer to the backend.
- The backend evaluates and stores every correct or incorrect attempt.
- The practice screen displays the cumulative history for the current question.
- Added Flyway `V6__create_question_answer_history.sql` for PostgreSQL and H2.
- Added APIs to submit an attempt, list history, and read a per-question summary.

create index if not exists idx_answer_attempt_session_time
    on question_answer_attempts(session_id, answered_at desc);

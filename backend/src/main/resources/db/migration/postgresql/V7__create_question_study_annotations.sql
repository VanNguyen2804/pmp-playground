-- Stores one personal study note and text highlight ranges per question.
create table if not exists question_study_annotations (
    id bigserial primary key,
    question_id bigint not null,
    note_text text,
    highlights_json text not null,
    version bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_study_annotation_question unique (question_id),
    constraint fk_study_annotation_question
        foreign key (question_id) references pmp_questions(id) on delete cascade
);

create unique index if not exists idx_study_annotation_question
    on question_study_annotations(question_id);

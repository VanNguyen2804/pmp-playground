-- Safe upgrade for databases created before the explanation-review feature.

alter table pmp_questions add column if not exists ai_explanation text;
alter table pmp_questions add column if not exists final_explanation text;
alter table pmp_questions add column if not exists final_explanation_source varchar(20);
alter table pmp_questions add column if not exists explanation_review_status varchar(20);
alter table pmp_questions add column if not exists explanation_review_notes text;
alter table pmp_questions add column if not exists explanation_reviewed_at timestamptz;

update pmp_questions
set final_explanation_source = 'NONE'
where final_explanation_source is null;

update pmp_questions
set explanation_review_status = 'PENDING'
where explanation_review_status is null;

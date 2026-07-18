create index if not exists idx_question_external_id on pmp_questions(external_id);
create index if not exists idx_question_type on pmp_questions(question_type);
create index if not exists idx_question_source on pmp_questions(source);
create index if not exists idx_question_option_question on question_options(question_id);
create index if not exists idx_matching_pair_question on matching_pairs(question_id);
create index if not exists idx_question_category_question on question_categories(question_id);
create index if not exists idx_question_category_category on question_categories(category_id);

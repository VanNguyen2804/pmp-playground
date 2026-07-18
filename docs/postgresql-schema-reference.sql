-- Reference only. Flyway owns schema changes; Hibernate validates the resulting schema.
create table categories (
  id bigserial primary key,
  code varchar(80) not null unique,
  name varchar(160) not null,
  description text,
  taxonomy varchar(30) not null,
  display_order integer not null,
  active boolean not null default true,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table pmp_questions (
  id bigserial primary key,
  external_id varchar(80) unique,
  exam_name varchar(180),
  question_type varchar(20) not null,
  question_text text not null,
  image_url text,
  pma_explanation text,
  ai_explanation text,
  final_explanation text,
  final_explanation_source varchar(20),
  explanation_review_status varchar(20),
  explanation_review_notes text,
  explanation_reviewed_at timestamptz,
  explanation_status varchar(30) not null,
  explanation_type varchar(40),
  explanation_prompt_version integer,
  number_of_answers integer,
  source varchar(20) not null,
  difficulty varchar(20),
  reference text,
  tags text,
  raw_source_json text,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table question_options (
  id bigserial primary key,
  question_id bigint not null references pmp_questions(id) on delete cascade,
  option_key varchar(10) not null,
  option_text text not null,
  display_order integer not null,
  correct boolean not null
);

create table matching_pairs (
  id bigserial primary key,
  question_id bigint not null references pmp_questions(id) on delete cascade,
  left_text text not null,
  right_text text not null,
  display_order integer not null
);

create table question_categories (
  question_id bigint not null references pmp_questions(id) on delete cascade,
  category_id bigint not null references categories(id) on delete cascade,
  primary key (question_id, category_id)
);

create index idx_question_external_id on pmp_questions(external_id);
create index idx_question_type on pmp_questions(question_type);
create index idx_question_source on pmp_questions(source);
create index idx_question_category_category on question_categories(category_id);

-- Added by Flyway V6.
create table question_answer_attempts (
    id bigserial primary key,
    question_id bigint not null references pmp_questions(id) on delete cascade,
    question_type varchar(20) not null,
    submitted_answer_json text not null,
    correct boolean not null,
    session_id varchar(80),
    answered_at timestamptz not null
);

create index idx_answer_attempt_question_time
    on question_answer_attempts(question_id, answered_at desc);

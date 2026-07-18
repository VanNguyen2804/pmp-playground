-- Current application schema for a brand-new PostgreSQL/Neon database.
-- Existing non-empty databases are baselined at version 1 and start with V2.

create table categories (
    id bigserial primary key,
    code varchar(80) not null,
    name varchar(160) not null,
    description text,
    taxonomy varchar(30) not null,
    display_order integer not null,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_category_code unique (code),
    constraint categories_taxonomy_check
        check (taxonomy in ('PMP_TOPIC', 'PMBOK8_DOMAIN', 'PMA_HANDOUT_TOPIC'))
);

create table pmp_questions (
    id bigserial primary key,
    external_id varchar(80),
    exam_name varchar(180),
    question_type varchar(20) not null,
    question_text text not null,
    image_url text,
    pma_explanation text,
    ai_explanation text,
    final_explanation text,
    final_explanation_source varchar(20) default 'NONE',
    explanation_review_status varchar(20) default 'PENDING',
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
    updated_at timestamptz not null,
    constraint uk_question_external_id unique (external_id)
);

create table question_options (
    id bigserial primary key,
    question_id bigint not null,
    option_key varchar(10) not null,
    option_text text not null,
    display_order integer not null,
    correct boolean not null,
    constraint fk_question_options_question
        foreign key (question_id) references pmp_questions(id) on delete cascade
);

create table matching_pairs (
    id bigserial primary key,
    question_id bigint not null,
    left_text text not null,
    right_text text not null,
    display_order integer not null,
    constraint fk_matching_pairs_question
        foreign key (question_id) references pmp_questions(id) on delete cascade
);

create table question_categories (
    question_id bigint not null,
    category_id bigint not null,
    constraint uk_question_category unique (question_id, category_id),
    constraint fk_question_categories_question
        foreign key (question_id) references pmp_questions(id) on delete cascade,
    constraint fk_question_categories_category
        foreign key (category_id) references categories(id) on delete cascade
);

create index idx_question_external_id on pmp_questions(external_id);
create index idx_question_type on pmp_questions(question_type);
create index idx_question_source on pmp_questions(source);
create index idx_question_option_question on question_options(question_id);
create index idx_matching_pair_question on matching_pairs(question_id);
create index idx_question_category_question on question_categories(question_id);
create index idx_question_category_category on question_categories(category_id);

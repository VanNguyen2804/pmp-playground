-- Older versions of the application used @Lob for String fields. On PostgreSQL,
-- those fields could be created as bytea. Text search then failed with:
--   ERROR: function lower(bytea) does not exist
--
-- Convert every legacy bytea/oid text field to PostgreSQL text. The migration is
-- safe for new databases because columns that are already text are left unchanged.

do $$
declare
    target record;
    current_type text;
begin
    for target in
        select *
        from (values
            ('categories', 'description'),
            ('pmp_questions', 'question_text'),
            ('pmp_questions', 'image_url'),
            ('pmp_questions', 'pma_explanation'),
            ('pmp_questions', 'ai_explanation'),
            ('pmp_questions', 'final_explanation'),
            ('pmp_questions', 'explanation_review_notes'),
            ('pmp_questions', 'reference'),
            ('pmp_questions', 'tags'),
            ('pmp_questions', 'raw_source_json'),
            ('question_options', 'option_text'),
            ('matching_pairs', 'left_text'),
            ('matching_pairs', 'right_text')
        ) as columns_to_fix(table_name, column_name)
    loop
        select c.udt_name
          into current_type
          from information_schema.columns c
         where c.table_schema = current_schema()
           and c.table_name = target.table_name
           and c.column_name = target.column_name;

        if current_type = 'bytea' then
            execute format(
                'alter table %I alter column %I type text using convert_from(%I, ''UTF8'')',
                target.table_name,
                target.column_name,
                target.column_name
            );
        elsif current_type = 'oid' then
            execute format(
                'alter table %I alter column %I type text using convert_from(lo_get(%I), ''UTF8'')',
                target.table_name,
                target.column_name,
                target.column_name
            );
        end if;
    end loop;
end $$;

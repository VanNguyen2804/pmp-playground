alter table categories drop constraint if exists categories_taxonomy_check;

update categories
set taxonomy = 'PMP_TOPIC'
where taxonomy is null
   or taxonomy not in ('PMP_TOPIC', 'PMBOK8_DOMAIN', 'PMA_HANDOUT_TOPIC');

alter table categories
    add constraint categories_taxonomy_check
    check (taxonomy in ('PMP_TOPIC', 'PMBOK8_DOMAIN', 'PMA_HANDOUT_TOPIC'));

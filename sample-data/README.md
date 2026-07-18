# Sample data

- `exam1.json`: PMA exam-export structure. Upload it from the **Upload PMA** page or POST it to `/api/questions/import/pma-exam`.
- `pmp_questions.csv`: legacy CSV sample.
- `pmp_questions.json`: generic API JSON sample; update it to the new normalized request shape before importing.

Example upload:

```bash
curl -X POST http://localhost:8080/api/questions/import/pma-exam \
  -F "file=@sample-data/exam1.json"
```

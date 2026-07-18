# PMP Question Bank

A lightweight full-stack application for storing, importing, maintaining, and practicing PMP multiple-choice questions.

## Stack

- Frontend: Angular 21 standalone components
- Backend: Java 21, Spring Boot 3.5
- Database: H2 file database (no separate database server)
- Import formats: CSV and JSON

## Main features

- Create, edit, delete, search, and filter PMP questions
- Upload questions in CSV or JSON format
- Persistent local H2 database in `backend/data/`
- Random practice mode with answer checking and explanations
- H2 console for local inspection

## Run backend

Requirements: Java 21 and Maven 3.6.3+

```bash
cd backend
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

H2 console: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:file:./data/pmpdb`
- User: `sa`
- Password: empty

## Run frontend

Requirements: Node.js compatible with Angular 21 and npm.

```bash
cd frontend
npm install
npm start
```

Frontend URL: `http://localhost:4200`

## CSV format

Required headers:

```text
questionText,optionA,optionB,optionC,optionD,correctOption
```

Optional headers:

```text
explanation,category,difficulty,source,reference,tags
```

- `correctOption`: A, B, C, or D
- `difficulty`: EASY, MEDIUM, or HARD
- Quote fields that contain commas or line breaks.

Sample files are in `sample-data/`.

## API summary

- `GET /api/questions`
- `GET /api/questions/{id}`
- `POST /api/questions`
- `PUT /api/questions/{id}`
- `DELETE /api/questions/{id}`
- `GET /api/questions/random?count=10`
- `POST /api/questions/import/csv`
- `POST /api/questions/import/json`

## Database upgrade path

H2 is suitable for a personal/local MVP. To deploy for multiple users, replace H2 with PostgreSQL by changing the JDBC dependency and datasource configuration; the entity and repository layers can remain largely unchanged.

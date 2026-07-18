# PMP Playground

A lightweight full-stack PMP question bank with Angular, Spring Boot, and PostgreSQL/H2 support.

## Stack

- Frontend: Angular 21 standalone components
- Backend: Java 21 and Spring Boot 3.5
- Local database: H2 file database
- Production database: PostgreSQL, prepared for Neon
- Deployment: Render Blueprint (`render.yaml`)
- CI: GitHub Actions

## Features

- Create, edit, delete, search, and filter PMP questions
- Upload questions in CSV or JSON format
- Random practice mode with answer checking and explanations
- Local hot reload for Angular and Spring Boot DevTools
- Automatic deployment from GitHub through Render
- Runtime frontend API configuration, generated from environment variables

## Repository structure

```text
pmp-playground/
├── frontend/
├── backend/
├── sample-data/
├── render.yaml
└── .github/workflows/ci.yml
```

## Local development

### Backend

Requirements: Java 21 and Maven.

```bash
cd backend
mvn spring-boot:run
```

The default `local` profile uses H2:

- API: `http://localhost:8080/api/questions`
- H2 console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/pmpdb`
- Username: `sa`
- Password: empty

Spring Boot DevTools restarts the backend after compiled classpath changes.

### Frontend

Requirements: Node.js 22 and npm.

```bash
cd frontend
npm ci
npm start
```

Open `http://localhost:4200`. Angular automatically reloads after source changes.

Before starting or building, `scripts/generate-config.mjs` creates `public/app-config.json`. Locally it defaults to `http://localhost:8080/api`.

## Production deployment on Render + Neon

### 1. Create a Neon PostgreSQL database

Create a Neon project in a region close to the Render backend. Obtain:

- host/database JDBC URL
- username
- password

Use a JDBC URL similar to:

```text
jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
```

### 2. Push this repository to GitHub

```bash
git add .
git commit -m "Prepare PMP Playground for Render and Neon"
git push origin main
```

### 3. Deploy the Render Blueprint

In Render:

1. Connect your GitHub account.
2. Select **New → Blueprint**.
3. Select this repository.
4. Render reads `render.yaml` and creates:
   - `pmp-playground-api`: Spring Boot Docker web service
   - `pmp-playground-web`: Angular static site
5. Enter the three requested secret values:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
6. Deploy the Blueprint.

The Blueprint automatically passes the backend hostname to Angular at build time. The backend is configured for the expected frontend URL `https://pmp-playground-web.onrender.com`, and SPA routes are rewritten to `index.html`. If Render adds a suffix to the frontend service name, update `CORS_ALLOWED_ORIGINS` in the backend service settings.

## Automatic deployment

Every push to `main` triggers:

1. GitHub Actions builds the frontend and tests the backend.
2. Render detects the commit and rebuilds the affected service.
3. Because each Render service has a separate `rootDir`, frontend-only changes do not rebuild the backend, and backend-only changes do not rebuild the frontend.

This is continuous deployment, not browser hot reload. For immediate hot reload, run the project locally.

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
- Quote fields containing commas or line breaks.

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
- `GET /actuator/health`

## Security notes

Never commit database passwords, GitHub tokens, or production secrets. Enter secrets only in the Render dashboard or another secret manager.

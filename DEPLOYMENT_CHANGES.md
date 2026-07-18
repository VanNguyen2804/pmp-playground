# Deployment changes

- Added PostgreSQL support for Neon while retaining H2 for local development.
- Added Spring Boot local/prod profiles.
- Added configurable CORS for the Render frontend hostname.
- Added Spring Boot Actuator health endpoint.
- Added multi-stage Dockerfile for the backend.
- Added runtime Angular API configuration generated from Render environment variables.
- Added Render Blueprint for a monorepo with separate frontend/backend services.
- Added SPA rewrite rules and basic static-site security headers.
- Added GitHub Actions build workflow.
- Added `.env.example` files and expanded `.gitignore`.

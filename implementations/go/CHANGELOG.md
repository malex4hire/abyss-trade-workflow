# Maintainability refactor

- Split the React component into API, hook, form, types and presentation components.
- Replace overlapping interval polling with completion-based polling and cancellation.
- Separate Go startup, HTTP handling, database transactions and Kafka processing.
- Format Go and frontend source. Add ESLint and Prettier configuration.
- Commit go.sum and use the npm lockfile for reproducible dependency installation.
- Use host ports 13000, 18080 and 18081 consistently in docs, UI and verification scripts.
- Use independent Compose project name abyss-trade-mnt.
- Add frontend tests, HTTP contract tests and PostgreSQL concurrency/deduplication tests.
- Preserve the business workflow and schema. Do not add business uniqueness or create-request idempotency.

Small corrections included with the refactor: exact decimal parsing, clearer submitted-state text, cancellation-aware shutdown, invalid mode validation and retry delay on Kafka fetch errors. Unexpected database failures now consistently return 503 through the API.

Swagger UI can now call the API from local port 18081 through an explicit loopback-only CORS policy.

# Validation of the maintainable edition

Passed in the authoring environment:

- Go compilation with the locked module dependencies (Go 1.24.9).
- Go unit tests, including HTTP validation, submission-response semantics, Swagger preflight and malformed event handling.
- Go race checks and vet for the application packages.
- Compilation of the PostgreSQL integration test suite with its integration build tag.
- Frontend strict TypeScript checking and production bundle.
- ESLint and Prettier checks.
- Fourteen frontend tests covering decimal conversion, HTTP errors, polling, cancellation and stale responses.
- YAML parsing, Python smoke-test syntax and shell-script syntax.

Not executed here:

- Full Docker/PostgreSQL/Kafka smoke test, because Docker is unavailable.
- PostgreSQL transaction integration tests. They compile and are configured as a Compose test service, but require a running PostgreSQL instance.
- Container security scans, Kubernetes deployment or live broker recovery.

Run `./demo`, `python3 scripts/smoke.py`, and
`docker compose --profile test run --build --rm tests` on the Docker-enabled host.
The integration test distinguishes repeated creation of identical data from
repeated submission of one ID and duplicate processing of one event ID.

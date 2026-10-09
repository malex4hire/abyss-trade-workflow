# Validation

Verified in the build environment with Temurin Java 25.0.4.1 and Maven 3.9.11:

- All 32 main Java source files and six test source files compile for Java 25.
- Eleven backend tests pass: service idempotency, threshold rule, MVC authentication/validation/status handling and a real Tomcat HTTP request that asserts virtual-thread execution.
- Java formatter check and Maven packaging pass.
- Fourteen frontend tests, ESLint, Prettier, TypeScript checking and production build pass.

Docker is unavailable in this environment. The PostgreSQL Testcontainers test is skipped explicitly; the Compose/Kafka smoke, recovery procedure, image scan and Kubernetes deployment have not been executed here. Run `./mvnw verify` with Docker available and `python3 scripts/smoke.py` against the running stack to verify the database concurrency and live event flow.

The generated-password log in the MVC slice test belongs to test-only auto-configuration. The actual application excludes the default user-details auto-configuration and uses the demo bearer filter.

## Event subscription update

15 Java tests and 16 frontend tests passed, plus Java packaging and frontend lint/build. Docker/Kafka end-to-end verification is pending. This package is standalone: Compose project abyss-trade-java-events, new data volumes, host ports 13000/18080/18081.

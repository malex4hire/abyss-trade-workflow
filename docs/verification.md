# Verification and limitations

Java checks passed 15 tests, 16 frontend tests, Java packaging, frontend lint and production build. A real Tomcat request confirmed virtual-thread execution. Unit and HTTP tests cover completion handling, authentication, duplicate events and browser subscriptions.

A live Java/PostgreSQL/Kafka run verified:

- No repeated declaration reads while the portal is idle.
- Submission remains pending while the worker is stopped.
- Repeating submission for the same ID returns the existing state.
- Worker restart delivers the decision through one persistent SSE subscription.
- Audit history contains one submission and one decision.

CI runs Java verification, PostgreSQL Testcontainers checks, Compose smoke checks, and the Go unit, race, transaction and smoke checks. A configured CI job is not evidence that its latest run passed; check GitHub Actions for results.

Multi-replica fan-out, browser event replay, Kafka broker failover, load capacity and Kubernetes operation remain unverified. Smoke checks create synthetic records. Neither implementation establishes business uniqueness for identical draft data.

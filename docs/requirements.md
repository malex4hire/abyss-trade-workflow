# Requisition mapping

| Capability | Evidence | Boundary |
|---|---|---|
| Java and Spring Boot | pom.xml, TradeApplication, MVC controllers | Java 25 is the chosen implementation target |
| REST and OpenAPI | openapi.yaml, DeclarationController, Swagger UI | No generated client/server interface pipeline |
| Relational data and transactions | JDBC repositories, DeclarationService, EventProcessingService | PostgreSQL only, no JPA/Hibernate demo |
| Event-driven integration | Spring Kafka submission/completion listeners, outbox publisher | Kafka only, no JMS integration |
| Duplicate handling and recovery | Row locks, processed_events, outbox, Kafka acknowledgments | No business uniqueness rule or create-request idempotency key |
| Schema migration | Flyway V1/V2 | Production migration ownership needs agreement |
| React and TypeScript | web/src components, hooks and API modules | Demo portal, not a full developer catalog |
| Automated testing | JUnit, MockMvc, Testcontainers, Vitest, live browser verification | Live test status comes from CI runs |
| Docker and CI/CD | Dockerfiles, compose.yaml, GitHub Actions | No executed Harness pipeline |
| Kubernetes | deploy/kubernetes.yaml | Starter manifests, no cluster validation |
| Authentication and authorization | DemoTokenFilter, owner-scoped queries and notifications | Shared synthetic identity, no OAuth/OIDC deployment |
| Audit and observability | Audit history, request IDs, virtual-thread header, process logs | No distributed tracing or production monitoring platform |

JMS, AI-assisted test generation, enterprise API catalogs and production identity integration remain future scope. The client must define actual trade fields, rules, uniqueness, amendments and access policies.

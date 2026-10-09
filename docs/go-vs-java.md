# Go and Java tradeoffs

The earlier Go edition and this Java edition share the transaction, outbox and duplicate-event design. The Go baseline uses browser polling. Both source implementations are included under `implementations/`. The Java edition adds Kafka completion events and SSE. Go can implement the same delivery path.

| Area | Go baseline | Java event edition |
|---|---|---|
| HTTP | net/http and goroutines | Spring MVC and virtual threads |
| Database | pgx and explicit transactions | Spring JDBC and transactional services |
| Messaging | kafka-go with explicit handling | Spring Kafka listeners and error handler |
| Migrations | SQL database initialization | Flyway versioned migrations |
| Packaging | Compiled binary | Executable JAR and Java 25 runtime |
| Portal status | Polling | Kafka completion consumer and SSE |

Java fits the requested Spring ecosystem and supplies transaction, messaging, validation and security infrastructure. Go offers compact executable packaging and explicit control flow. How much code each needs depends on selected libraries and scope, not just the language.

Choose from measured requirements: memory per replica, startup constraints, CPU, p95/p99 latency, integration complexity, operational diagnostics and maintenance work. No equivalent benchmark establishes a performance winner here. Database capacity, Kafka throughput and slow browser connections can dominate either implementation.

React and virtual threads are different layers. React renders the browser interface. Virtual threads run blocking Java request work. SSE changes how browser status updates arrive. Neither Java edition uses WebFlux or structured concurrency.

Run from `implementations/java/`; use `implementations/go/README.md` for Go.

# Trade demo — Java 25

A maintainable single-workflow demonstration: create a trade declaration draft, submit it, and receive an asynchronous accepted/rejected decision in the React portal.

The backend uses **Java 25, Spring Boot 3.5.16, Spring MVC, virtual threads, JDBC, PostgreSQL, Flyway and Spring Kafka**. It does not use WebFlux. Separate API, publisher and worker processes run from the same Java codebase with different Spring profiles.

## Start in WSL or Linux

Install Docker with Compose and make sure Docker is running. You do not need Java or Node installed for the container demo.

First stop the previous demo from its own directory:

```sh
cd /path/to/your/previous-demo
docker compose down
```

Extract this ZIP into a new `trade-demo-java-events` directory. The ZIP contains the project files directly, without an extra enclosing directory.

```sh
cd /path/to/trade-demo-java-events
chmod +x demo mvnw scripts/recovery.sh
./demo
docker compose logs -f api publisher worker
```

The first build downloads Maven dependencies and builds the React portal. Wait until the API and worker have started. `./demo` generates a random demo token in `.env` on first use. Copy its value into the portal; the token represents one synthetic user, `demo-importer`.

| Endpoint | Address |
| --- | --- |
| Portal | http://localhost:13000 |
| API health | http://localhost:18080/health |
| OpenAPI documentation | http://localhost:18081 |

The API root `/` is not an application page and returns 404. Use `/health` or `/api/declarations`. Host ports are 13000, 18080 and 18081; container ports remain 80, 8080 and 8080 respectively. PostgreSQL and Kafka are internal to the Compose network.

This project has the Compose name `abyss-trade-java-events` and its own database and Kafka volumes. Stopping the old demo avoids host port collisions. Existing Go data is not automatically imported or baselined into Flyway.

```sh
# Stop this Java demo, retaining data:
docker compose down

# Rebuild after a Java source or configuration change:
docker compose up --build -d api publisher worker

# Rebuild after a React change:
docker compose up --build -d portal
```

## Walk through the workflow

Enter an importer, two-letter uppercase origin country, six-digit commodity code, USD value and shipment reference. Save a draft, then submit that declaration. The portal opens one authenticated SSE connection and refreshes when the API signals a completion.

The illustrative worker rule accepts values up to and including $100,000 and rejects larger values. This is a demonstration threshold, not a customs or trade compliance determination.

### What repeat requests do

- **Save draft twice:** two declarations, with different IDs, even if every field and the token are identical. Business uniqueness and create-request idempotency are not defined.
- **Submit the same ID repeatedly:** the first transition returns HTTP 202; subsequent requests return HTTP 200 with the existing status. One database row lock serializes requests for that ID. Only the first transition inserts the submission event and audit entry.
- **Deliver the same event ID repeatedly:** one transaction inserts the processed event ID, updates the decision and records its audit. The event ID primary key prevents a second decision for a redelivery.

A row lock is not a duplicate-data rule. Before rejecting a newly created declaration as a duplicate, the client must define the identity fields, ownership scope, time window, amendments and allowed resubmissions. No such rule has been invented here.

## Architecture and transaction boundaries

The browser calls the portal's same-origin `/api` proxy. Nginx forwards requests to the Spring MVC API. Swagger UI calls the host API directly with a narrowly scoped local CORS policy.

1. A validated MVC request enters `DeclarationService`. JDBC repositories own SQL; domain records and rules do not depend on HTTP or Kafka.
2. Submission locks the declaration with `SELECT ... FOR UPDATE`. The status change, audit entry and outbox event commit in one PostgreSQL transaction.
3. The publisher claims an unpublished outbox row using `FOR UPDATE SKIP LOCKED`, sends it to `trade.submitted`, waits for the broker acknowledgment, then marks it published. A failure rolls the transaction back for a later retry.
4. The Spring Kafka worker commits its processed-event marker, decision and audit together. The listener returns only after the service transaction commits; record acknowledgment then allows Kafka offset advancement.
5. The worker commits its decision, audit and `trade.completed` outbox event together. The publisher delivers it to Kafka. Each API replica consumes completion events and notifies its local SSE connections; React then reads the durable status and audit trail.

Kafka delivery is **at least once**. A crash after publishing but before marking the outbox can publish again; a crash after database commit but before offset commit can redeliver. Database idempotency handles these windows. This is not an end-to-end exactly-once transport claim.

Malformed event payloads go to `trade.deadletter`. Other processing failures retry with a one-second backoff; persistent failures can block a partition until repaired. Dead-letter publication failure is not treated as successful recovery. Production needs explicit retry limits, alerting, replay procedures and poison-event policy.

Spring MVC uses regular imperative controllers and blocking JDBC. `spring.threads.virtual.enabled=true` configures virtual threads for HTTP handling. The `/health` response includes `X-Virtual-Thread`; a real HTTP test asserts that it is `true`. Virtual threads do not increase database pool capacity: Hikari is limited to ten connections per Java process.

## Code map

| Location | Responsibility |
| --- | --- |
| `src/main/java/com/abyssapplied/trade/api` | HTTP routes, validation errors and health |
| `.../config` | Demo bearer authentication, CORS, tracing and properties |
| `.../domain` | Declaration records, statuses, event contract and illustrative rule |
| `.../service` | Transaction boundaries, submit transition and event processing |
| `.../repository` | JDBC queries, JSON serialization, outbox and event markers |
| `.../messaging` | Kafka publisher, listener, topic names and error recovery |
| `src/main/resources/application*.yml` | Shared settings and API/publisher/worker profiles |
| `src/main/resources/db/migration` | Flyway versioned schema migration |
| `src/test/java` | HTTP, service, rule and PostgreSQL integration tests |
| `web/src` | Separate React components, API client, types and workflow hook |
| `openapi.yaml` | Explicit REST API contract and Swagger UI specification |
| `deploy` | Kubernetes starter manifests |
| `.github/workflows/ci.yml` | Build, formatting, tests, Compose smoke and image scan |

Modify business rules in `TradeRules`, orchestration in services, SQL in repositories, and presentation in React components. Add schema changes as new numbered Flyway files; do not edit an already applied migration.

## Development and verification

For local backend work, install **JDK 25** and set `JAVA_HOME`. The Maven wrapper downloads Maven 3.9.11.

```sh
./mvnw test                    # Unit/API tests and a real HTTP virtual-thread test
./mvnw spotless:check verify   # Formatting, packaging, PostgreSQL integration test
./mvnw spotless:apply          # Apply the Java formatter
```

`WorkflowIT` uses Testcontainers and requires Docker. It checks eight concurrent submits of one ID, duplicate event processing and separate IDs for identical drafts. It is explicitly skipped when Docker is unavailable. Passing unit tests alone does not verify the PostgreSQL locks or the live Kafka pipeline.

The frontend uses Node 22:

```sh
cd web
npm ci
npm run format:check
npm run lint
npm test
npm run build
```

With the Compose stack running:

```sh
python3 scripts/smoke.py
curl -i http://localhost:18080/health
./scripts/recovery.sh
```

The smoke script checks bearer authentication, invalid input, missing records, both decisions, eight concurrent submits and unchanged audit after another submit. The recovery script stops the worker so a submitted declaration remains pending; restart it with `docker compose start worker`.

## Fit to the requisition

This demonstrates Java/Spring Boot, Spring MVC, REST/OpenAPI, Spring JDBC, relational transactions and locking, Flyway, Spring Kafka, automated tests, React/TypeScript, Docker and a CI pipeline. The checked-in OpenAPI contract is maintained alongside the controllers; client/server generation is not implemented.

It does not demonstrate JPA/Hibernate, JMS, automated AI test generation, Arazzo, an enterprise API catalog or a full production API governance process. Compose is local orchestration, not a simulation of Kubernetes. The Kubernetes files are deployment starters and require real infrastructure, images, broker/database configuration, secrets and operational verification.

The local demo token, synthetic trade rule, plaintext internal broker, sample database credentials and single-node infrastructure are for demonstration. Production work includes real OIDC/OAuth identity and authorization, approved duplicate rules, migrations and backup recovery, TLS, managed secrets, observability, scaling, accessibility verification and program-specific security requirements.

Reference: [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

## Persistent status subscription

`GET /api/events` requires the same bearer token as the other API routes. React uses streaming fetch, since native EventSource cannot set an Authorization header. There is one connection per mounted portal session, independent of the selected declaration. Heartbeats every 15 seconds keep Nginx from timing out; they do not trigger reads. Notifications trigger snapshot reads; initial load, selection, manual refresh and local mutations also read state. There are no timed status requests.

The completion path is PostgreSQL transactional outbox → Kafka `trade.completed` → API notification consumer → SSE. Notifications are scoped to the event owner. Every API process uses its own consumer group so all replicas can notify their own connections. Redelivery is harmless: it refreshes state, never repeats a business decision. Reconnection uses backoff and refreshes a snapshot to recover notifications missed while disconnected. SSE delivers invalidations, not a durable replay of every transition. Drafts created in another tab become visible on refresh; completion notifications refresh the list.

For a production notification fleet, use managed stable replica/group identities, retention policies, bounded fanout and backpressure, connection limits, and consumer-lag monitoring. This demo uses a unique group on each process start and reads retained completion events, favoring recovery over avoiding replay. PostgreSQL remains the authoritative snapshot.

This is a separate demo. Extract into a new `trade-demo-java-events` directory and run `./demo` there. It uses the Compose project `abyss-trade-java-events`, separate images, and new PostgreSQL/Kafka volumes. It generates its own `.env` token. Stop any previous demo before starting because the host ports are shared. Existing declarations are not imported.

Flyway V2 adds outbox routing without deleting existing records. kafka-init creates completion and dead-letter topics. Ports remain 13000 / 18080 / 18081. In browser Network, `/api/events` stays open; idle heartbeats should cause no list/detail requests. Submit a draft: a completion notification refreshes the decision. Stop the worker before submitting, then restart it to show recovery.

Validation for this update: 15 Java tests and 16 frontend tests pass. The real HTTP test still confirms virtual-thread execution. Frontend lint and production build pass. A live Java/PostgreSQL/Kafka browser run verified completion delivery over SSE and worker restart recovery; multi-replica and load testing remain outstanding.

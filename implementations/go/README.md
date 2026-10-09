# Abyss Applied trade demo: maintainable edition

One workflow: create a draft, submit it, process an event, and display the decision.
Synthetic data and illustrative validation only. No agency connection or authoritative customs decision.

## Start the Go edition

Stop the original demo from its original directory first:

```sh
cd /path/to/trade-demo
docker compose down
```

Extract this package into your `trade-demo-mnt` directory, then:

```sh
cd implementations/go
./demo
```

Requires Docker with Compose v2. The launcher creates a `.env` token using OpenSSL. Without OpenSSL, copy `.env.example` to `.env` and set the token first. Go and Node do not need to be installed locally.

| Surface    | Host URL                      | Container address |
| ---------- | ----------------------------- | ----------------- |
| Portal     | http://localhost:13000        | portal:80         |
| API health | http://localhost:18080/health | api:8080          |
| Swagger UI | http://localhost:18081        | docs:8080         |

Paste `DEMO_TOKEN` from `.env` into the portal. Use Swagger's Authorize button for API requests. The API root `/` has no handler and returns 404. Check `/health` instead.

Compose uses project name `abyss-trade-mnt`, so its database and Kafka volumes are separate from the original demo. Stopping the original preserves its data, but this new project starts with its own data. Host port changes do not require changing internal container addresses.

## Code map

### Frontend

| File or directory                           | Responsibility                                           |
| ------------------------------------------- | -------------------------------------------------------- |
| `web/src/main.tsx`                          | Mount the application                                    |
| `web/src/App.tsx`                           | Compose the page, hold token and selected declaration ID |
| `web/src/api/client.ts`                     | HTTP requests, bearer headers, errors and cancellation   |
| `web/src/api/declarations.ts`               | Typed declaration endpoint calls                         |
| `web/src/hooks/useDeclarations.ts`          | Polling, loading/error state and mutations               |
| `web/src/components/DeclarationForm.tsx`    | Explicit, labeled input fields                           |
| `web/src/components/DeclarationList.tsx`    | List and selection                                       |
| `web/src/components/DeclarationDetails.tsx` | Status and submit action                                 |
| `web/src/components/AuditTrail.tsx`         | Audit display                                            |
| `web/src/components/AccessTokenInput.tsx`   | Demo token input and docs link                           |
| `web/src/forms/declaration.ts`              | Form defaults and decimal-to-cents conversion            |
| `web/src/types/declaration.ts`              | API and form types                                       |

Polling waits until a request finishes before scheduling the next poll. Changing the selection or token cancels obsolete requests. Responses from an old selection cannot overwrite the current details. Mutations share a busy guard to prevent simultaneous button actions in the portal. That guard does not establish business uniqueness.

### Go

| File or directory                | Responsibility                                                  |
| -------------------------------- | --------------------------------------------------------------- |
| `cmd/trade/main.go`              | Process startup, mode selection and graceful HTTP shutdown      |
| `internal/config`                | Environment settings and mode validation                        |
| `internal/httpapi`               | Routes, authentication, input validation and response contracts |
| `internal/trade`                 | Business data, status constants and illustrative rules          |
| `internal/store/declarations.go` | Draft creation and declaration reads                            |
| `internal/store/submission.go`   | Row lock, submitted status, audit and outbox transaction        |
| `internal/store/outbox.go`       | Pending-event claim and acknowledged publication                |
| `internal/store/processing.go`   | Event deduplication and decision transaction                    |
| `internal/messaging`             | Kafka publisher, consumer, retry and dead-letter behavior       |
| `internal/identifier`            | UUID generation and validation                                  |

The API depends on a small repository interface. HTTP tests can validate contracts without Kafka or PostgreSQL. Store integration tests use actual PostgreSQL transactions.

## Duplicate behavior, precisely

- Every Save draft call creates a new declaration ID, including identical data.
- Multiple submit requests for **one declaration ID** produce one submission event. A row lock makes concurrent requests observe the first committed status.
- Multiple deliveries of **one event ID** cannot repeat the decision. The processed-event ID, decision and audit entry commit together.
- Business uniqueness and create-request idempotency remain undefined. No new unique constraint or duplicate-rejection rule has been introduced.

The backend returns 202 for a new submission and 200 for a repeated submit of an existing ID. Repeated creation of identical data remains allowed.

## Verify

With the stack running:

```sh
python3 scripts/smoke.py
# Real PostgreSQL transaction tests, isolated test-owned rows only:
docker compose --profile test run --build --rm tests
```

The smoke test checks real HTTP/Kafka processing, accepted/rejected results, and concurrent submits of one ID. Store integration tests check one outbox event after eight concurrent submits, one decision after duplicate processing of the same event ID, and separate IDs for repeated draft creation. The transaction test invokes the database processor directly; it does not claim to test Kafka redelivery itself.

With local development tools:

```sh
# Go 1.24 or later
go test -race ./cmd/... ./internal/...
go vet ./cmd/... ./internal/...
go build ./cmd/trade
gofmt -w cmd internal

# Node 22 or later
cd web
npm ci
npm run lint
npm test
npm run build
npm run format:check
# After editing files:
npm run format
```

Frontend tests cover exact decimal conversion, HTTP errors, nonoverlapping polling, cancellation and stale responses. CI also configures Compose smoke tests, PostgreSQL transaction tests and security scans.

## Live demonstration

1. Save a draft and submit it. Observe accepted status and audit entries.
2. Use a value above $100,000 to show a rejection reason.
3. Run `docker compose stop worker`, then create and submit another draft.
4. Run `docker compose start worker`. Observe processing resume.
5. Follow event/declaration IDs with `docker compose logs -f api publisher worker`.

The $100,000 rule is invented. Country and commodity checks verify format only. Transport is at least once, with idempotent database side effects. Broker failure leaves the outbox pending. Malformed events go to the dead-letter topic. Persistent database/valid-message failures retry and can block a consumer; bounded retries, replay tooling and alerts remain future work.

## Rebuild and stop

```sh
# Frontend changes
docker compose up -d --build portal
# Go changes
docker compose up -d --build api publisher worker
# All changes
docker compose up -d --build
# Stop and preserve data
docker compose down
```

Hard-refresh the browser after rebuilding the portal. Changing OpenAPI on disk can require `docker compose restart docs` to reload the Swagger configuration. Avoid `down -v` unless intentionally deleting demo data.

## Deployment scope

Docker Compose runs the local demo. `deploy/kubernetes.yaml` contains starter Go workload manifests, not a simulated or complete Kubernetes deployment. Supply PostgreSQL/Kafka, a `trade-runtime` secret, pushed images, database migrations, portal deployment and ingress for a real cluster.

`migrations/001_initial.sql` executes only when PostgreSQL initializes a new volume. Its schema remains compatible with the original demo. Future schema updates require a migration runner.

Shared bearer authentication models one demo identity. Real OIDC, RBAC, organization isolation, Java/Spring, JMS, Harness-specific configuration and AI test generation remain separate work.

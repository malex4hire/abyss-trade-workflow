Java source paths below are relative to `implementations/java/`. Go baseline source is in `implementations/go/`.

# Architecture and event distribution

## Submission and decision

`DeclarationController` delegates to `DeclarationService`. Creating a draft writes its record and first audit entry. Submitting locks the row and commits status, audit and a `trade.submitted` outbox record in one transaction. Repeat submits for the same ID return the existing state.

`OutboxService` locks an unpublished outbox row. `OutboxPublisher` sends its payload to the row's topic with its declaration ID as the Kafka key. It waits for broker acknowledgment before marking publication. A crash between acknowledgment and commit can redeliver.

`SubmissionListener` consumes submission events. `EventProcessingService` inserts the processed event ID, locks the declaration, applies the illustrative rule and writes the decision, audit and a `trade.completed` outbox event in one transaction. The listener returns after commit, then advances its Kafka offset. Duplicate event IDs do not repeat the decision.

## Browser notifications

`CompletionListener` runs in the API process. Each API replica uses a separate Kafka consumer group, so every replica receives completion events. `DeclarationSubscription` matches each event owner to its local SSE connections. Sharing a group would load-balance events to one replica and miss clients attached to others.

`GET /api/events` authenticates with the same bearer token as the other API routes. React's `subscription.ts` uses streaming fetch because native EventSource cannot attach an Authorization header. A session keeps one stream while selection changes. Nginx disables buffering and allows enough idle time for the heartbeat.

The stream sends `reset`, `changed` and `heartbeat` events. Reset and change trigger a current-state read. Heartbeats do not. Reconnect uses backoff. Invalid credentials stop automatic retries. Token replacement or component unmount cancels the old connection.

## Durability and capacity limits

PostgreSQL and Kafka provide the durable workflow path. SSE connections have no durable per-browser log or acknowledgment. A reconnect reads current state and may miss intermediate transitions. Repeat completion notifications cause reads without repeating business work.

The demo creates a unique group per API process start and replays retained completion events. Production should deliberately manage group identities, offset retention and replica lifecycles. The current synchronous fan-out needs bounded queues, slow-client handling and connection limits for a large fleet. A dedicated notification service is an alternative. Neither this code nor virtual threads establishes tested user capacity.

The demo token maps every authorized session to one synthetic importer. Owner filtering illustrates a boundary but does not demonstrate multi-tenant authorization.

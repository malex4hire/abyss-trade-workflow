# Java edition

- Replaced the Go backend with Java 25 and Spring Boot 3.5.16 using Spring MVC and virtual threads.
- Separated HTTP controllers, domain types, transaction services, JDBC repositories and Kafka components.
- Added Flyway migrations, Java formatting and tests, including real HTTP virtual-thread verification and a Docker-backed PostgreSQL concurrency test.
- Retained the modular React frontend, workflow and API contract.
- Set host ports to portal 13000, API 18080 and documentation 18081; gave this edition its own Compose project and volumes.
- Added Java container builds, CI checks and updated Kubernetes starters.

Repeat submission of one declaration ID and repeat delivery of one event ID remain idempotent. Identical draft data still creates separate IDs; a business duplicate rule awaits requirements.

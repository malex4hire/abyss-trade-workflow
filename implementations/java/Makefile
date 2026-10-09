.PHONY: up down verify
up:
	./demo
down:
	docker compose down
verify:
	./mvnw -B -ntp spotless:check verify
	cd web && npm ci && npm run lint && npm test && npm run build
	./demo
	python3 scripts/smoke.py

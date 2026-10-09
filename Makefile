.PHONY: up down verify
EDITION ?= java
up:
	./demo $(EDITION)
down:
	cd implementations/$(EDITION) && docker compose down
verify:
	$(MAKE) -C implementations/$(EDITION) verify

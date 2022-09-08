GITHUB_SHA=HEAD
VERSION=1.0-SNAPSHOT
MAVEN_CLI_OPTS?=-s .m2/settings.xml
LEVANT_VERSION=0.3.1

.PHONY:	clean
clean:
	docker-compose down --remove-orphans --volumes
	mvn $(MAVEN_CLI_OPTS) clean

.PHONY: install
install:
	mvn $(MAVEN_CLI_OPTS) install
	docker-compose build --build-arg VERSION="$(VERSION)" --build-arg GIT_REV="$(GITHUB_SHA)" --build-arg BUILD_DATE="$(shell date)"

.PHONY:	format
format:
	mvn $(MAVEN_CLI_OPTS) ktlint:format

.PHONY:	run
run: format install
	docker-compose up -d

.PHONY:	images
images:	install
	docker tag ghcr.io/noumenadigital/seed/api:latest ghcr.io/noumenadigital/seed/api:$(VERSION)
	docker push ghcr.io/noumenadigital/seed/api:$(VERSION)

	docker tag ghcr.io/noumenadigital/seed/engine:latest ghcr.io/noumenadigital/seed/engine:$(VERSION)
	docker push ghcr.io/noumenadigital/seed/engine:$(VERSION)

define deploy
	docker run --rm -v $(CURDIR)/nomad:/jobs:ro --network=host \
		hashicorp/levant:$(LEVANT_VERSION) levant deploy \
			-address $(NOMAD_ADDR) \
			-ignore-no-changes \
			-force-count \
			-var 'version=$(VERSION)' \
			-var-file /jobs/env-$(ENVIRONMENT).yml \
			/jobs/$1.nomad
endef

define run_batch
	# levant has issues with batch files, so we just deploy them and ignore failures
	-docker run --rm -v $(CURDIR)/nomad:/jobs:ro --network=host \
		hashicorp/levant:$(LEVANT_VERSION) levant deploy \
			-address $(NOMAD_ADDR) \
			-ignore-no-changes \
			-force-count \
			-var 'version=${VERSION}' \
			-var-file /jobs/env-$(ENVIRONMENT).yml \
			/jobs/$1.nomad
endef

.PHONY:	deploy
deploy:
	@if [[ "$(VERSION)" = "latest" ]] || [[ "$(VERSION)" = "" ]]; then echo "Explicit VERSION not set"; exit 1; fi
	@if [[ "$(ENVIRONMENT)" = "" ]]; then echo "ENVIRONMENT not set"; exit 1; fi
	$(call deploy,keycloak)
	$(call run_batch,keycloak-provisioning)
	$(call deploy,platform)
	$(call deploy,api)

.PHONY:	deploy-dev
deploy-dev:	export NOMAD_ADDR=https://nomad.seed-dev.noumenadigital.com
deploy-dev:	export ENVIRONMENT=dev
deploy-dev:	deploy

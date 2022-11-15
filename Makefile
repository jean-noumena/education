GITHUB_SHA=HEAD
VERSION=1.0-SNAPSHOT
MAVEN_CLI_OPTS?=-s .m2/settings.xml
LEVANT_VERSION=0.3.1
# test
.PHONY: sonar-scan
sonar-scan:
	mvn $(MAVEN_CLI_OPTS) -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=NoumenaDigital_seed

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

	docker tag ghcr.io/noumenadigital/seed/keycloak-provisioning:latest ghcr.io/noumenadigital/seed/keycloak-provisioning:$(VERSION)
	docker push ghcr.io/noumenadigital/seed/keycloak-provisioning:$(VERSION)

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

.PHONY: clean-nomad
clean-nomad:
	@if [[ "$(ENVIRONMENT)" = "" ]]; then echo "ENVIRONMENT not set"; exit 1; fi
	-nomad stop -yes -purge platform
	-nomad stop -yes -purge keycloak
	-nomad stop -yes -purge keycloak-provisioning
	-nomad stop -yes -purge api
	$(call deploy,cleanup)

.PHONY: clean-dev
clean-dev:	export NOMAD_ADDR=https://nomad.seed-dev.noumenadigital.com
clean-dev:	export ENVIRONMENT=dev
clean-dev: clean-nomad

.PHONY: run-integration-test
run-integration-test: export SEED_TEST_USER=system
run-integration-test: export SEED_TEST_PASSWORD=welcome
run-integration-test:
	docker-compose up -d
	mvn $(MAVEN_CLI_OPTS) -am clean integration-test verify -Pintegration-test -pl it-test
	docker-compose down --volumes

.PHONY: integration-test
integration-test: clean install run-integration-test

.PHONY: login
login:
	echo $(MAVEN_REPO_PASS) | docker login ghcr.io -u $(MAVEN_REPO_USER) --password-stdin

.PHONY: docker-scan-login
docker-scan-login:
	docker scan --login
.PHONY: snyk-scan
snyk-scan: 
	docker scan ghcr.io/noumenadigital/seed/api:latest
	docker scan ghcr.io/noumenadigital/seed/engine:latest


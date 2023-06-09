GITHUB_SHA=HEAD
VERSION?=1.0-SNAPSHOT
MAVEN_CLI_OPTS?=-s .m2/settings.xml
LEVANT_VERSION=0.3.2

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
	docker-compose -f docker-compose.yml -f docker-compose-infra.yml build --build-arg VERSION="$(VERSION)" --build-arg GIT_REV="$(GITHUB_SHA)" --build-arg BUILD_DATE="$(shell date)"

.PHONY:	format
format:
	mvn $(MAVEN_CLI_OPTS) ktlint:format

.PHONY:	run-only
run-only:
	docker-compose up -d

.PHONY:	run
run: format install run-only

.PHONY:	stop
stop:
	docker-compose down

.PHONY:	images
images:	install
	docker tag ghcr.io/noumenadigital/seed/infra-provisioning:latest ghcr.io/noumenadigital/seed/infra-provisioning:$(VERSION)
	docker push ghcr.io/noumenadigital/seed/infra-provisioning:$(VERSION)

	docker tag ghcr.io/noumenadigital/seed/keycloak-provisioning:latest ghcr.io/noumenadigital/seed/keycloak-provisioning:$(VERSION)
	docker push ghcr.io/noumenadigital/seed/keycloak-provisioning:$(VERSION)

	docker tag ghcr.io/noumenadigital/seed/engine:latest ghcr.io/noumenadigital/seed/engine:$(VERSION)
	docker push ghcr.io/noumenadigital/seed/engine:$(VERSION)

	docker tag ghcr.io/noumenadigital/seed/api:latest ghcr.io/noumenadigital/seed/api:$(VERSION)
	docker push ghcr.io/noumenadigital/seed/api:$(VERSION)

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

.PHONY:	deploy
deploy:
	@if [ "$(VERSION)" = "latest" ] || [ "$(VERSION)" = "" ]; then echo "Explicit VERSION not set"; exit 1; fi
	@if [ "$(ENVIRONMENT)" = "" ]; then echo "ENVIRONMENT not set"; exit 1; fi
	$(call deploy,keycloak)
	-$(call deploy,keycloak-provisioning)
	$(call deploy,platform)
	$(call deploy,postgraphile)
	$(call deploy,history)
	$(call deploy,api)


.PHONY:	deploy-shared
deploy-shared:
	nomad namespace apply -description "Seed DEV environment" "seed"
	-$(call deploy,infra-provisioning)

.PHONY:	deploy-dev
deploy-dev:	export NOMAD_ADDR=https://nomad.seed-dev.noumenadigital.com
deploy-dev:	export ENVIRONMENT=dev
deploy-dev:	deploy

.PHONY:	deploy-shared-dev
deploy-shared-dev: export NOMAD_ADDR=https://nomad.shared-dev.noumenadigital.com
deploy-shared-dev: export ENVIRONMENT=shared-dev
deploy-shared-dev: deploy-shared deploy

.PHONY: clean-nomad
clean-nomad:
	@if [ "$(ENVIRONMENT)" = "" ]; then echo "ENVIRONMENT not set"; exit 1; fi
	@if [ "$(NOMAD_NAMESPACE)" = "" ]; then echo "NOMAD_NAMESPACE not set"; exit 1; fi
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge api
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge history
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge postgraphile
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge platform
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge keycloak-provisioning
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge keycloak
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge infra-provisioning
	-nomad stop -namespace $(NOMAD_NAMESPACE) -yes -purge cleanup
	-@if nomad namespace list | grep -q "$(NOMAD_NAMESPACE)"; then $(call deploy,cleanup); fi

.PHONY: clean-dev
clean-dev: export NOMAD_ADDR=https://nomad.seed-dev.noumenadigital.com
clean-dev: export NOMAD_NAMESPACE=default
clean-dev: export ENVIRONMENT=dev
clean-dev: clean-nomad

.PHONY: clean-shared-dev
clean-shared-dev: export NOMAD_ADDR=https://nomad.shared-dev.noumenadigital.com
clean-shared-dev: export NOMAD_NAMESPACE=seed
clean-shared-dev: export ENVIRONMENT=shared-dev
clean-shared-dev: clean-nomad

.PHONY: run-integration-test
run-integration-test: run-only
	mvn $(MAVEN_CLI_OPTS) -am clean integration-test verify -Pintegration-test -pl it-test
	docker-compose down --volumes

.PHONY: integration-test
integration-test: clean install run-integration-test

.PHONY: login
login:
	echo $(GITHUB_USER_PASS) | docker login ghcr.io -u $(GITHUB_USER_NAME) --password-stdin

.PHONY: docker-scan-login
docker-scan-login:
	docker scan --login

.PHONY: snyk-scan
snyk-scan:
	docker scan ghcr.io/noumenadigital/seed/api:latest
	docker scan ghcr.io/noumenadigital/seed/engine:latest

dependency-report: dependency-report/target/site/dependencies.html

dependency-report/target/site/dependencies.html:
	@if [ "$(VERSION)" = "latest" ] || [ "$(VERSION)" = "" ]; then echo "Explicit VERSION not set"; exit 1; fi
	mvn versions:set ${MAVEN_CLI_OPTS} -DnewVersion=$(VERSION) -DprocessAllModules
	mvn ${MAVEN_CLI_OPTS} install -DskipTests
	mvn ${MAVEN_CLI_OPTS} -pl dependency-report site

.PHONY:	bump-platform-version
bump-platform-version:
	@if [ "$(PLATFORM_VERSION)" = "" ]; then echo "PLATFORM_VERSION not set"; exit 1; fi
	perl -p -i -e's/PLATFORM_VERSION=.*/PLATFORM_VERSION=$(PLATFORM_VERSION)/' .env
	perl -p -i -e's/platform_version:.*/platform_version: $(PLATFORM_VERSION)/' nomad/env*.yml
	perl -p -i -e's|FROM ghcr.io/noumenadigital/packages/engine:.*|FROM ghcr.io/noumenadigital/packages/engine:$(PLATFORM_VERSION)|' npl/Dockerfile


.PHONY:	slack
slack:
	@if [ "$(MESSAGE)" = "" ]; then echo "MESSAGE not set"; exit 1; fi
	@if [ "$(SLACK_URL)" = "" ]; then echo "SLACK_URL not set"; exit 1; fi
	curl -s -H "Content-Type: application/json" $(SLACK_URL) --data '{"username": "Github", "channel": "#platform", "text": "$(MESSAGE)"}'

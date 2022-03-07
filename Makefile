CI_COMMIT_SHA=HEAD
VERSION=1.0-SNAPSHOT

.PHONY:	clean
clean:
	docker-compose down --remove-orphans --volumes
	mvn $(MAVEN_CLI_OPTS) clean

.PHONY: install
install:
	mvn $(MAVEN_CLI_OPTS) install
	docker-compose build --build-arg VERSION="$(VERSION)" --build-arg GIT_REV="$(CI_COMMIT_SHA)" --build-arg BUILD_DATE="$(shell date)"

.PHONY:	format
format:
	mvn $(MAVEN_CLI_OPTS) ktlint:format

.PHONY:	run
run: format install
	docker-compose up -d


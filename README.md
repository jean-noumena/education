# Seed

The seed project is an example on how to set up new NPL-based projects.

## Documentation

Project Managers must periodically review the data protection and operation document to make sure that the information
is up-to-date.

### Data protection

[Data protection](DATA_PROTECTION.md) page contain a checklist which must be updated before production. It contains a
list of data protection / GDPR issues which must be considered if the product is going to production.

### Operations

[Operations](OPERATIONS.md) page contains information which are needed to support the application in production or
production-like environment.

## Running

### Preparations

In order to run the seed project, make sure you have the following:

* access to our [GitHub Packages](https://github.com/noumenadigital/packages) repository
* an environment variable called `GITHUB_REPO_USER` which is set to your GitHub account name
* an environment variable called `GITHUB_REPO_PASS` which is set to a GitHub Personal Access Token with at least
  the `read packages` permission
* Java 17
* Maven
* Docker

### Building

```shell
$ make login  
```

```shell
$ make clean install  
```

### Running

```shell
$ make run
```

Verify that everything is running correctly by verifying the health checks:

* API: `curl -v http://localhost:8000/health`
* Keycloak: `curl -v http://localhost:11000/health`
* Engine: `curl -v http://localhost:12000/actuator/health`
* History: `curl -v http://localhost:12010/actuator/health`

Check the metrics on

* API: `curl http://localhost:8000/metrics`

## Playing around

When the seed project is running, you may communicate with the API using swagger, or through commands using curl.

Swagger UI for the API is available at http://localhost:8080/swagger/.
Swagger UI for the engine is available at http://localhost:12000/swagger-ui/.
Swagger UI for the history is available at http://localhost:12010/swagger-ui/.

### Swagger

In order to communicate with the API, you must authenticate to the API. This can be done through Swagger with the
following sample credentials.

* payee1:welcome1
* payee2:welcome2
* issuer1:welcome3

After authenticating, you may issue commands to the API.

### curl

In order to communicate with the API, you must authenticate to the API and retrieve your access token. You can
authenticate using the following command with one of the previously specified credentials.

```shell
# - via api:
curl -X 'POST' \
  'http://localhost:8080/auth/login' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "grant_type": "password",
    "username": "issuer1",
    "password": "welcome3"
  }'
````

The response to this request is a JSON object.

```json
{
  "access_token": "ACCESS_TOKEN",
  "expires_in": 300,
  "refresh_token": "REFRESH_TOKEN"
}
```

The `ACCESS_TOKEN` value is needed to communicate with the API.

The following command will authenticate to the API and extract the `ACCESS_TOKEN` value into an environment variable.

```shell
# - via raw keycloak:
export ACCESS_TOKEN=$(curl -s 'http://localhost:11000/realms/seed/protocol/openid-connect/token' \
 -H 'Content-Type: application/x-www-form-urlencoded' \
 -d 'username=issuer1' \
 -d 'password=welcome3' \
 -d 'grant_type=password' \
 -d 'client_id=seed' | grep -Eo '"access_token":.*?[^\\]"' | sed 's/.*:"\(.*\)"/\1/')
```

After authenticating to the API, the following commands can be used to test out the API.

```shell
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X POST  http://localhost:8080/iou/100/issuer1
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X GET   http://localhost:8080/iou/{protocolId}/amountOwed
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X PATCH http://localhost:8080/iou/{protocolId}/pay/1
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X PUT   http://localhost:8080/iou/{protocolId}/forgive
```

After deploying the history application, the following command can be used to test out the history API. Note that only
those data already copied to the history data store will be returned.

```shell
curl http://localhost:12711/admin/history/streams/states/{protocolId}
```

## On seed-dev

When the seed project is deployed to `seed-dev.noumenadigital.com`, you can execute the same commands:

```shell
export ACCESS_TOKEN=$(curl -X POST https://api.seed-dev.noumenadigital.com/auth/login -H "Content-Type: application/x-www-form-urlencoded" -d "username=USERNAME&password=PASSWORD&grant_type=password" | grep -Eo '"access_token":.*?[^\\]"' | sed 's/.*:"\(.*\)"/\1/')
```

```shell
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X POST  https://api.seed-dev.noumenadigital.com/iou/100/issuer1
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X GET   https://api.seed-dev.noumenadigital.com/iou/{protocolId}/amountOwed
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X PATCH https://api.seed-dev.noumenadigital.com/iou/{protocolId}/pay/1
curl -H "Authorization: Bearer "$ACCESS_TOKEN"" -X PUT   https://api.seed-dev.noumenadigital.com/iou/{protocolId}/forgive                                                  
```

## Streams

Refer to the official docs at https://docs.core.noumenadigital.com/docs/operating-engine/APIs/.

Observe the various raw stream types using the following `curl` commands:

```shell
# get the JWT ACCESS_TOKEN
# - via raw keycloak:
export ACCESS_TOKEN=$(curl -s 'http://localhost:11000/realms/seed/protocol/openid-connect/token' \
-d 'username=issuer1' \
-d 'password=welcome3' \
-d 'client_id=seed' \
-d 'grant_type=password' | jq -j .access_token)

# - via api:
export ACCESS_TOKEN=$(curl -X 'POST' \
  'http://localhost:8080/auth/login' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "grant_type": "password",
    "username": "issuer1",
    "password": "welcome3"
  }' | jq -j .access_token)

# Start listening to SSE stream
curl -H "accept: text/event-stream" -H "authorization: Bearer "$ACCESS_TOKEN"" -X GET http://localhost:8080/iou/sse

# Listen to a stream type:
# - all
curl -H "accept: text/event-stream" -H "authorization: Bearer "$ACCESS_TOKEN"" -X GET http://localhost:12000/api/streams
# - notifications
curl -H "accept: text/event-stream" -H "authorization: Bearer "$ACCESS_TOKEN"" -X GET http://localhost:12000/api/streams/notifications?me=false
# - prototypes
curl -H "accept: text/event-stream" -H "authorization: Bearer "$ACCESS_TOKEN"" -X GET http://localhost:12000/api/streams/prototypes
# - states
curl -H "accept: text/event-stream" -H "authorization: Bearer "$ACCESS_TOKEN"" -X GET http://localhost:12000/api/streams/states?me=false
# - commands
curl -H "accept: text/event-stream" -H "authorization: Bearer "$ACCESS_TOKEN"" -X GET http://localhost:12000/api/streams/commands?me=false
```

## IT tests
#### NPL Migration Test

There is an example NPL migration test here - `it-test/src/test/kotlin/engine/EngineTest.kt`.  
It is currently disabled.

Supporting files are found in `it-test/src/test/resources/engine/`.
- `kotlin-script` is the migration script
- `npl-1.0.0/nxtlog` is NPL at version 1.0.0
- `npl-1.0.1/nxtlog` is NPL at version 1.0.1
- `migration.yml` is the migration descriptor file

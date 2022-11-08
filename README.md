# Seed

The seed project is an example on how to set up new NPL-based projects.

## Documentation 

Project Managers must periodically review the data protection and operation document to make sure that the information is up-to-date.

### Data protection
DATA_PROTECTION.md contain a checklist which must be updated before production. It contains a list of data protection / GDPR issues which must be considered if the product is going to production.

### Operations
OPERATIONS.md contains information which are needed to support the application in production or production-like environment.

## Running 

### Preparations

In order to run the seed project, make sure you have the following: 

* access to our [GitHub Packages](https://github.com/noumenadigital/packages) repository
* an environment variable called `MAVEN_REPO_USER` which is set to your GitHub account name
* an environment variable called `MAVEN_REPO_PASS` which is set to a GitHub Personal Access Token with at least the `read packages` permission
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

Check the metrics on 

* API: `curl http://localhost:8000/metrics`

## Playing around

When the seed project is running, you may communicate with the API using swagger, or through commands using curl.

### Swagger
Swagger is available at http://localhost:8080/swagger/

In order to communicate with the API, you must authenticate to the API. This can be done through Swagger with the following sample credentials.
* payee1:welcome1
* payee2:welcome2
* issuer1:welcome3

After authenticating, you may issue commands to the API.

### curl
In order to communicate with the API, you must authenticate to the API and retrieve your access token. You can authenticate using the following command with one of the previously specified credentials.

`curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/x-www-form-urlencoded" -d "username={USERNAME}&password={PASSWORD}&grant_type=password"`

The response to this request is a JSON object.

```json
{
  "access_token" : "ACCESS_TOKEN",
  "expires_in" : 300,
  "refresh_token" : "REFRESH_TOKEN"
}
```

The `ACCESS_TOKEN` value is needed to communicate with the API.

The following command will authenticate to the API and extract the `ACCESS_TOKEN` value into an environment variable.

```shell
export ACCESS_TOKEN=$(curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/x-www-form-urlencoded" -d "username=USERNAME&password=PASSWORD&grant_type=password" | grep -Eo '"access_token":.*?[^\\]"' | sed 's/.*:"\(.*\)"/\1/')
```

After authenticating to the API, the following commands can be used to test out the API.

```shell
curl -X POST -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/raw/iou/100/issuer1
curl -X GET -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/raw/iou/amountOwed/{protocolId}
curl -X POST -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/raw/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/raw/iou/forgive/{protocolId}

curl -X POST -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/gen/iou/100/issuer1
curl -X GET -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/gen/iou/amountOwed/{protocolId}
curl -X POST -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/gen/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST -H "Authorization: Bearer "$ACCESS_TOKEN"" http://localhost:8080/gen/iou/forgive/{protocolId}
```

## On seed-dev

When the seed project is deployed to seed-dev.noumenadigital.com, you can execute the same commands: 

```shell
curl -X POST https://api.seed-dev.noumenadigital.com/raw/iou/100
curl -X GET https://api.seed-dev.noumenadigital.com/raw/iou/amountOwed/{protocolId}
curl -X POST https://api.seed-dev.noumenadigital.com/raw/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST https://api.seed-dev.noumenadigital.com/raw/iou/forgive/{protocolId}



curl -X POST https://api.seed-dev.noumenadigital.com/gen/iou/100
curl -X GET https://api.seed-dev.noumenadigital.com/gen/iou/amountOwed/{protocolId}
curl -X POST https://api.seed-dev.noumenadigital.com/gen/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST https://api.seed-dev.noumenadigital.com/gen/iou/forgive/{protocolId}
```

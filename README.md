# Seed

The seed project is an example on how to set up new NPL-based projects.

## Running 

### Preparations

In order to run the seed project, make sure you have the following: 

* access to our [GitHub Packages](https://github.com/noumenadigital/packages) repository
* an environment variable called `MAVEN_USER_NAME` which is set to your GitHub account name
* an environment variable called `MAVEN_USER_PASS` which is set to a GitHub Personal Access Token with at least the `read packages` permission
* Java 17
* Maven 
* Docker 

### Building

```shell
$ make clean install  
```

### Running 

```shell
$ make run
```

Verify that everything is running correctly by verifying the health checks:

* API: `curl -v http://localhost:9100/health` 
* Keycloak: `curl -v http://localhost:11000/health`
* Engine: `curl -v http://localhost:12000/actuator/health`

Check the metrics on 

* API: `curl http://localhost:9100/metrics`

## Playing around

When the seed project is running, you can execute the following commands to communicate with the API: 

```shell
curl -X POST http://localhost:9000/raw/iou/100
curl -X GET http://localhost:9000/raw/iou/amountOwed/{protocolId}
curl -X POST http://localhost:9000/raw/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST http://localhost:9000/raw/iou/forgive/{protocolId}



curl -X POST http://localhost:9000/gen/iou/100
curl -X GET http://localhost:9000/gen/iou/amountOwed/{protocolId}
curl -X POST http://localhost:9000/gen/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST http://localhost:9000/gen/iou/forgive/{protocolId}
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

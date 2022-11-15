# Contact information
For any security issues contact security@noumenadigital.com

# Security scanning
## Code Scanning
Code is scanned by default by SonarCloud.io and Pull Requests are decorated with the result.

## Docker Scanning 
**We do not have the ability to automate this with current license, please run this periodically**

Docker containers can be scanned with 
```
# Login to the app. First time requires registration with DockerHub account.
make docker-scan-login
# Scan containers. Note -i switch to ignore errors as otherwise vulnerability would result in exit from make
make -i snyk-scan 

```
This requires you to login to app.snyk.io with your Dockerhub credentials.

## DAST
TODO

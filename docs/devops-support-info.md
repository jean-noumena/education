# Seed: Support information

**KEEP THIS DOCUMENT UP-TO-DATE**

This document offers the devops team tools and direct links which are required to support the application in case something crashes or doesn't work.
## Documentation and links
* Generic
    * VPN | Seed dev: https://noumenadigital.atlassian.net/wiki/spaces/DL/pages/3055714519/VPN#Seed-dev-vpn 
    * Vault: https://vault.seed-dev.noumenadigital.com
    * Nomad: https://nomad.seed-dev.noumenadigital.com
    * Prometheus: https://prometheus.seed-dev.noumenadigital.com/graph
    * KeyCloak: https://keycloak.seed-dev.noumenadigital.com/
    * Kibana: https://kibana.seed-dev.noumenadigital.com/app/kibana#/home?_g=()
    * Grafana: https://grafana.seed-dev.noumenadigital.com/login
    * Consul: https://consul.seed-dev.noumenadigital.com/ui/seed-dev/services
    * AlertManager: https://alertmanager.seed-dev.noumenadigital.com/#/alerts
    * Traefik: https://traefik.seed-dev.noumenadigital.com/
* IP Space: 10.16.0.0/16
* App specific Documentation
    * Add any important app specific docs and links here
    * Examples:
        * Links to BlockChain accounts
        * Links to how-to docs regarding things like smart contract deployment etc.
        * Links app specific documentation
        * Link to base infra customization
        * Links to a list explaining what all the docker containers in /nomad are doing.
* Development and repos
    * Backend: https://github.com/NoumenaDigital/seed
    * Frontend: If frontend is in a separate repo, give link here
    * Any other dependencies?
* Contractual documents
    * **If we have documentation regarding SLAs, response times, what services we are offering, add them here.**

## Contacts
List any relevant contacts here. **This is important!**
* Project manager: Faisal, faisal@noumenadigital.com
* DevOps and Developers: 
    * DevOps: Ville, ville@noumenadigital.com
    * DevOps: Faisal, Faisal@noumenadigital.com
    * Backend development: Sandy, sandy@noumenadigital.com
* Client:
    * Technical Client contact(s): UPDATE THIS
    * Management: UPDATE THIS

## Infra
This project uses standard Noumena Infra with no changes

TODO: Add picture of infra

## Application

## List of containers

List of docker containers, what they do and where are they connecting. This is important information when debugging or creating firewall rules.

| Container              | Purpose                                                            | Incoming (ext traffic)  | Outgoing (ext traffic)  | Incoming (internal)    | Outgoing (internal)     |
|:-----------------------|:-------------------------------------------------------------------|:------------------------|:------------------------|:-----------------------|:------------------------|
| api-service            | The main API exposed to the internet                               | FrontEnd / internet     | None                    | None                   | Platform, Postgraphile  |
| Platform               | NPL Engine                                                         | None                    | SQL(5432)               | api-service            | None                    |
| Postgraphile           | DB query helper                                                    | None                    | SQL(5432)               | api-service            | None                    |
| KeyCloak               | Identity Management                                                | None                    | None                    | api-service, Platform  | None                    |
| Upshift                | DB migrations, run only once when migrated to another NPL version  | None                    | SQL server?             | None                   | Platform                |
| KeyCloak-Provisioning  | Initial KeyCloak Users                                             | None                    | None                    | None                   | KeyCloak                |


TODO: Add image or picture or diagram or jpeg of the application architecture where all the dockers are and which components they are supposed to talk to.


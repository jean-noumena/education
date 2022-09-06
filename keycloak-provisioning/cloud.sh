#!/bin/sh
set -e

cat > /terraform/backend.tf <<EOT
terraform {
  backend "consul" {
    address="consul.service.consul:8500"
    path="terraform/keycloak"
  }
}
EOT

terraform init
terraform apply -auto-approve

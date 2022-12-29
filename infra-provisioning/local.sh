#!/bin/sh
set -e

terraform apply -auto-approve -state=/state/state.tf 

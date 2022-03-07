FROM hashicorp/terraform:latest

ENV KEYCLOAK_USER="admin" \
    KEYCLOAK_PASSWORD="Seed123!" \
    KEYCLOAK_URL=http://keycloak:11000

WORKDIR /terraform
COPY providers.tf terraform.tf /terraform/
VOLUME /state

RUN terraform init
CMD ["apply", "-auto-approve", "-state=/state/state.tf"]

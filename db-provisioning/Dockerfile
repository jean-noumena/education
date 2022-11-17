FROM hashicorp/terraform:latest

ENV VAULT_ADDR=http://vault:8200 \
    VAULT_TOKEN=default \
    TF_VAR_postgres_host=postgres-v2.service.consul \
    TF_VAR_postgres_username=postgresql \
    TF_VAR_postgres_password=postgresql

WORKDIR /terraform
COPY providers.tf terraform.tf variables.tf /terraform/
COPY *.sh /
VOLUME /state

RUN terraform init
ENTRYPOINT []
CMD ["local.sh"]

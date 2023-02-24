job "cleanup" {
  type = "batch"
  datacenters = ["[[ .datacenter ]]"]
  namespace = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  group "cleanup" {
    task "postgres" {
      driver = "docker"
      config {
        image        = "postgres:[[ .postgres_version ]]"
        network_mode = "host"
        command      = "/local/script.sh"
      }
      template {
        destination = "${NOMAD_TASK_DIR}/script.sh"
        perms       = "755"
        data        = <<EOT
#!/bin/bash
{{ if secrets "secret/[[ .application_name ]]" | contains "[[ .platform_name ]]" }}{{ with secret "secret/[[ .application_name ]]/[[ .platform_name ]]" }}PGPASSWORD='{{ .Data.password }}' psql --set=sslmode=require -h postgres-v2.service.consul -U {{ .Data.username }} -c 'drop owned by current_user cascade' [[ .platform_database ]]{{ end }}{{ end }}
{{ if secrets "secret/[[ .application_name ]]" | contains "[[ .history_name ]]" }}{{ with secret "secret/[[ .application_name ]]/[[ .history_name ]]" }}PGPASSWORD='{{ .Data.password }}' psql --set=sslmode=require -h postgres-v2.service.consul -U {{ .Data.username }} -c 'drop owned by current_user cascade' [[ .platform_database ]]{{ end }}{{ end }}
{{ if secrets "secret/[[ .application_name ]]" | contains "[[ .keycloak_name ]]" }}{{ with secret "secret/[[ .application_name ]]/[[ .keycloak_name ]]" }}PGPASSWORD='{{ .Data.password }}' psql --set=sslmode=require -h postgres-v2.service.consul -U {{ .Data.username }} -c 'drop owned by current_user cascade' [[ .keycloak_database ]]{{ end }}{{ end }}
EOT
      }
    }
  }

  vault {
    policies = ["reader"]
  }
}

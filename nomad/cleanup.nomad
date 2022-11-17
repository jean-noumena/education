job "cleanup" {
  datacenters = [
    "[[ .datacenter ]]",
  ]

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  type = "batch"

  group "cleanup" {
    task "postgres-platform" {
      driver = "docker"
      config {
        image        = "postgres:11.3-alpine"
        network_mode = "host"
        command      = "/local/script.sh"
      }
      template {
        destination = "${NOMAD_TASK_DIR}/script.sh"
        perms       = "755"
        data        = <<EOT
#!/bin/bash
set -e
set -u
{{ with secret "secret/postgres-v2/[[ .platform_name ]]" }}PGPASSWORD="{{ .Data.password }}" psql -h postgres-v2.service.consul -U {{ .Data.username }} -c 'drop owned by current_user cascade' platform{{ end }}
{{ with secret "secret/postgres-v2/[[ .keycloak_name ]]" }}PGPASSWORD="{{ .Data.password }}" psql -h postgres-v2.service.consul -U {{ .Data.username }} -c 'drop owned by current_user cascade' keycloak{{ end }}
EOT
      }
    }
  }

  vault {
    policies = [
      "reader",
    ]
  }
}

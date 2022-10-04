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
{{ with secret "secret/postgres/platform" }}PGPASSWORD="{{ .Data.password }}" psql -h postgresql.service.consul -U {{ .Data.username }} -c 'drop owned by current_user cascade' platform{{ end }}
{{ with secret "secret/postgres/keycloak" }}PGPASSWORD="{{ .Data.password }}" psql -h postgresql.service.consul -U {{ .Data.username }} -c 'drop owned by current_user cascade' keycloak{{ end }}
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

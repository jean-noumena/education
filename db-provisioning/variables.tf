variable "postgres_host" {
  type        = string
  description = "Postgres host where to create databases and other users"
  default     = "postgres-v2.service.consul"
}

variable "postgres_username" {
  type        = string
  description = "Username of postgres admin user used to create databases and other users"
  default     = "postgres"
}

variable "postgres_password" {
  type        = string
  description = "Password for the admin user"
  sensitive   = true
}
terraform {
  backend "consul" {
    address="10.16.1.6:8500"
    path="terraform/db"
  }
}

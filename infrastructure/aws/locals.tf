locals {
  name_prefix = "${var.project_name}-${var.environment}"

  availability_zones = slice(
    data.aws_availability_zones.available.names,
    0,
    2
  )

  services = toset([
    "api-gateway",
    "user-service",
    "inventory-service",
    "order-service",
    "notification-service"
  ])

  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
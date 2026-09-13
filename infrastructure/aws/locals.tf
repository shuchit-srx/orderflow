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

  service_config = {
    api-gateway = {
      port      = 8080
      profile   = "local,docker"
      database  = null
      redis     = true
      rabbitmq  = false
      jwt       = "public"
      cpu       = 512
      memory    = 1024
      port_name = "gateway-http"
    }

    user-service = {
      port      = 8081
      profile   = "local"
      database  = "orderflow_user"
      redis     = false
      rabbitmq  = false
      jwt       = "private"
      cpu       = 512
      memory    = 1024
      port_name = "user-http"
    }

    inventory-service = {
      port      = 8082
      profile   = "local"
      database  = "orderflow_inventory"
      redis     = true
      rabbitmq  = false
      jwt       = "public"
      cpu       = 512
      memory    = 1024
      port_name = "inventory-http"
    }

    order-service = {
      port      = 8083
      profile   = "local"
      database  = "orderflow_order"
      redis     = false
      rabbitmq  = true
      jwt       = "public"
      cpu       = 512
      memory    = 1024
      port_name = "order-http"
    }

    notification-service = {
      port      = 8084
      profile   = "local"
      database  = "orderflow_notification"
      redis     = false
      rabbitmq  = true
      jwt       = "none"
      cpu       = 512
      memory    = 1024
      port_name = "notification-http"
    }
  }

  rabbitmq_endpoint = aws_mq_broker.rabbitmq.instances[0].endpoints[0]

  rabbitmq_host = split(
    ":",
    trimprefix(local.rabbitmq_endpoint, "amqps://")
  )[0]

  github_owner = split("/", var.github_repository)[0]
  github_repo  = split("/", var.github_repository)[1]

  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
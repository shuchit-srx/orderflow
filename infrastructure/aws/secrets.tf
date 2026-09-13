resource "random_password" "internal_service_token" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "internal_service_token" {
  name                    = "${var.project_name}/${var.environment}/internal-service-token"
  recovery_window_in_days = var.environment == "prod" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "internal_service_token" {
  secret_id     = aws_secretsmanager_secret.internal_service_token.id
  secret_string = random_password.internal_service_token.result
}

resource "aws_secretsmanager_secret" "rabbitmq" {
  name                    = "${var.project_name}/${var.environment}/rabbitmq"
  recovery_window_in_days = var.environment == "prod" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "rabbitmq" {
  secret_id = aws_secretsmanager_secret.rabbitmq.id

  secret_string = jsonencode({
    username = var.rabbitmq_username
    password = random_password.rabbitmq.result
    endpoint = aws_mq_broker.rabbitmq.instances[0].endpoints[0]
  })
}

resource "aws_secretsmanager_secret" "jwt_private_key" {
  name                    = "${var.project_name}/${var.environment}/jwt-private-key"
  recovery_window_in_days = var.environment == "prod" ? 30 : 0
}

resource "aws_secretsmanager_secret" "jwt_public_key" {
  name                    = "${var.project_name}/${var.environment}/jwt-public-key"
  recovery_window_in_days = var.environment == "prod" ? 30 : 0
}
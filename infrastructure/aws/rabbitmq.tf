resource "random_password" "rabbitmq" {
  length = 32

  special          = true
  override_special = "!@#$%^&*()-_+"

  min_lower   = 4
  min_upper   = 4
  min_numeric = 4
  min_special = 4
}

resource "aws_mq_broker" "rabbitmq" {
  broker_name = "${local.name_prefix}-rabbitmq"

  engine_type        = "RabbitMQ"
  engine_version     = var.rabbitmq_engine_version
  host_instance_type = var.rabbitmq_instance_type

  deployment_mode = "SINGLE_INSTANCE"

  publicly_accessible = false

  security_groups = [
    aws_security_group.rabbitmq.id
  ]

  subnet_ids = [
    aws_subnet.private_data[0].id
  ]

  auto_minor_version_upgrade = true
  apply_immediately          = true

  encryption_options {
    use_aws_owned_key = true
  }

  logs {
    general = true
  }

  user {
    username = var.rabbitmq_username
    password = random_password.rabbitmq.result
  }
}
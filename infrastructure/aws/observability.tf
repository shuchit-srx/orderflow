resource "aws_cloudwatch_log_group" "services" {
  for_each = local.services

  name = "/ecs/${local.name_prefix}/${each.key}"

  retention_in_days = var.cloudwatch_log_retention_days
}

resource "aws_cloudwatch_log_group" "database_bootstrap" {
  name = "/ecs/${local.name_prefix}/database-bootstrap"

  retention_in_days = var.cloudwatch_log_retention_days
}
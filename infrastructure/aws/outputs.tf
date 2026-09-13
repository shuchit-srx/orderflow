output "aws_region" {
  value = var.aws_region
}

output "vpc_id" {
  value = aws_vpc.main.id
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_data_subnet_ids" {
  value = aws_subnet.private_data[*].id
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_cluster_arn" {
  value = aws_ecs_cluster.main.arn
}

output "ecs_execution_role_arn" {
  value = aws_iam_role.ecs_execution.arn
}

output "ecs_task_role_arn" {
  value = aws_iam_role.ecs_task.arn
}

output "ecr_repository_urls" {
  value = {
    for name, repository in aws_ecr_repository.services :
    name => repository.repository_url
  }
}

output "alb_dns_name" {
  value = aws_lb.main.dns_name
}

output "gateway_target_group_arn" {
  value = aws_lb_target_group.gateway.arn
}

output "postgres_endpoint" {
  value = aws_db_instance.postgres.address
}

output "postgres_port" {
  value = aws_db_instance.postgres.port
}

output "postgres_master_secret_arn" {
  value = aws_db_instance.postgres.master_user_secret[0].secret_arn
}

output "redis_endpoint" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "redis_port" {
  value = aws_elasticache_replication_group.redis.port
}

output "rabbitmq_endpoint" {
  value = aws_mq_broker.rabbitmq.instances[0].endpoints[0]
}

output "rabbitmq_console_url" {
  value = aws_mq_broker.rabbitmq.instances[0].console_url
}

output "rabbitmq_secret_arn" {
  value = aws_secretsmanager_secret.rabbitmq.arn
}

output "internal_service_token_secret_arn" {
  value = aws_secretsmanager_secret.internal_service_token.arn
}

output "jwt_private_key_secret_arn" {
  value = aws_secretsmanager_secret.jwt_private_key.arn
}

output "jwt_public_key_secret_arn" {
  value = aws_secretsmanager_secret.jwt_public_key.arn
}
output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}

output "ecs_service_names" {
  value = {
    for name, service in aws_ecs_service.services :
    name => service.name
  }
}

output "database_bootstrap_task_family" {
  value = aws_ecs_task_definition.database_bootstrap.family
}

output "service_connect_namespace" {
  value = aws_service_discovery_http_namespace.service_connect.name
}

output "application_url" {
  value = var.enable_https ? "https://${var.domain_name}" : "http://${aws_lb.main.dns_name}"
}
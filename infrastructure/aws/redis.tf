resource "aws_elasticache_subnet_group" "redis" {
  name = "${local.name_prefix}-redis"

  subnet_ids = aws_subnet.private_data[*].id
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name_prefix}-redis"
  description          = "OrderFlow product cache"

  engine         = "redis"
  engine_version = var.redis_engine_version

  node_type = var.redis_node_type

  num_cache_clusters = 1

  port = 6379

  subnet_group_name = aws_elasticache_subnet_group.redis.name

  security_group_ids = [
    aws_security_group.redis.id
  ]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  automatic_failover_enabled = false
  multi_az_enabled           = false

  snapshot_retention_limit = 1

  apply_immediately = true
}
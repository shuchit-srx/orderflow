resource "aws_service_discovery_http_namespace" "service_connect" {
  name = "${local.name_prefix}-services"
}

locals {
  service_environment = {
    api-gateway = [
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "local,docker"
      },
      {
        name  = "SPRING_DATA_REDIS_HOST"
        value = aws_elasticache_replication_group.redis.primary_endpoint_address
      },
      {
        name  = "SPRING_DATA_REDIS_PORT"
        value = tostring(aws_elasticache_replication_group.redis.port)
      },
      {
        name  = "SPRING_DATA_REDIS_SSL_ENABLED"
        value = "true"
      },
      {
        name  = "SECURITY_JWT_PUBLIC_KEY_LOCATION"
        value = "file:/run/secrets/jwt/public.pem"
      },
      {
        name  = "SECURITY_JWT_ISSUER"
        value = "orderflow-user-service"
      }
    ]

    user-service = [
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "local"
      },
      {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/orderflow_user"
      },
      {
        name  = "SECURITY_JWT_PRIVATE_KEY_LOCATION"
        value = "file:/run/secrets/jwt/private.pem"
      },
      {
        name  = "SECURITY_JWT_PUBLIC_KEY_LOCATION"
        value = "file:/run/secrets/jwt/public.pem"
      },
      {
        name  = "SECURITY_JWT_ISSUER"
        value = "orderflow-user-service"
      }
    ]

    inventory-service = [
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "local"
      },
      {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/orderflow_inventory"
      },
      {
        name  = "SPRING_DATA_REDIS_HOST"
        value = aws_elasticache_replication_group.redis.primary_endpoint_address
      },
      {
        name  = "SPRING_DATA_REDIS_PORT"
        value = tostring(aws_elasticache_replication_group.redis.port)
      },
      {
        name  = "SPRING_DATA_REDIS_SSL_ENABLED"
        value = "true"
      },
      {
        name  = "SECURITY_JWT_PUBLIC_KEY_LOCATION"
        value = "file:/run/secrets/jwt/public.pem"
      },
      {
        name  = "SECURITY_JWT_ISSUER"
        value = "orderflow-user-service"
      }
    ]

    order-service = [
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "local"
      },
      {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/orderflow_order"
      },
      {
        name  = "SPRING_RABBITMQ_HOST"
        value = local.rabbitmq_host
      },
      {
        name  = "SPRING_RABBITMQ_PORT"
        value = "5671"
      },
      {
        name  = "SPRING_RABBITMQ_SSL_ENABLED"
        value = "true"
      },
      {
        name  = "INVENTORY_SERVICE_BASE_URL"
        value = "http://inventory-service:8082"
      },
      {
        name  = "INVENTORY_SERVICE_URL"
        value = "http://inventory-service:8082"
      },
      {
        name  = "SERVICES_INVENTORY_BASE_URL"
        value = "http://inventory-service:8082"
      },
      {
        name  = "SECURITY_JWT_PUBLIC_KEY_LOCATION"
        value = "file:/run/secrets/jwt/public.pem"
      },
      {
        name  = "SECURITY_JWT_ISSUER"
        value = "orderflow-user-service"
      }
    ]

    notification-service = [
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "local"
      },
      {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/orderflow_notification"
      },
      {
        name  = "SPRING_RABBITMQ_HOST"
        value = local.rabbitmq_host
      },
      {
        name  = "SPRING_RABBITMQ_PORT"
        value = "5671"
      },
      {
        name  = "SPRING_RABBITMQ_SSL_ENABLED"
        value = "true"
      }
    ]
  }

  service_secrets = {
    api-gateway = []

    user-service = [
      {
        name      = "SPRING_DATASOURCE_USERNAME"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:username::"
      },
      {
        name      = "SPRING_DATASOURCE_PASSWORD"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:password::"
      }
    ]

    inventory-service = [
      {
        name      = "SPRING_DATASOURCE_USERNAME"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:username::"
      },
      {
        name      = "SPRING_DATASOURCE_PASSWORD"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:password::"
      },
      {
        name      = "SECURITY_INTERNAL_TOKEN"
        valueFrom = aws_secretsmanager_secret.internal_service_token.arn
      }
    ]

    order-service = [
      {
        name      = "SPRING_DATASOURCE_USERNAME"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:username::"
      },
      {
        name      = "SPRING_DATASOURCE_PASSWORD"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:password::"
      },
      {
        name      = "SPRING_RABBITMQ_USERNAME"
        valueFrom = "${aws_secretsmanager_secret.rabbitmq.arn}:username::"
      },
      {
        name      = "SPRING_RABBITMQ_PASSWORD"
        valueFrom = "${aws_secretsmanager_secret.rabbitmq.arn}:password::"
      },
      {
        name      = "SECURITY_INTERNAL_TOKEN"
        valueFrom = aws_secretsmanager_secret.internal_service_token.arn
      }
    ]

    notification-service = [
      {
        name      = "SPRING_DATASOURCE_USERNAME"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:username::"
      },
      {
        name      = "SPRING_DATASOURCE_PASSWORD"
        valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:password::"
      },
      {
        name      = "SPRING_RABBITMQ_USERNAME"
        valueFrom = "${aws_secretsmanager_secret.rabbitmq.arn}:username::"
      },
      {
        name      = "SPRING_RABBITMQ_PASSWORD"
        valueFrom = "${aws_secretsmanager_secret.rabbitmq.arn}:password::"
      }
    ]
  }
}

resource "aws_ecs_task_definition" "services" {
  for_each = local.service_config

  family                   = "${local.name_prefix}-${each.key}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  cpu    = tostring(each.value.cpu)
  memory = tostring(each.value.memory)

  execution_role_arn = aws_iam_role.ecs_execution.arn
  task_role_arn      = aws_iam_role.ecs_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  dynamic "volume" {
    for_each = each.value.jwt == "none" ? [] : [1]

    content {
      name = "jwt-keys"
    }
  }

  container_definitions = jsonencode(
    concat(
      [
        {
          name      = each.key
          image     = "${aws_ecr_repository.services[each.key].repository_url}:bootstrap"
          essential = true

          portMappings = [
            {
              name          = each.value.port_name
              containerPort = each.value.port
              hostPort      = each.value.port
              protocol      = "tcp"
              appProtocol   = "http"
            }
          ]

          environment = local.service_environment[each.key]

          secrets = local.service_secrets[each.key]

          mountPoints = each.value.jwt == "none" ? [] : [
            {
              sourceVolume  = "jwt-keys"
              containerPath = "/run/secrets/jwt"
              readOnly      = true
            }
          ]

          dependsOn = each.value.jwt == "none" ? [] : [
            {
              containerName = "jwt-key-writer"
              condition     = "SUCCESS"
            }
          ]

          healthCheck = {
            command = [
              "CMD-SHELL",
              "curl --fail --silent http://localhost:${each.value.port}/actuator/health || exit 1"
            ]

            interval    = 30
            timeout     = 5
            retries     = 3
            startPeriod = 60
          }

          logConfiguration = {
            logDriver = "awslogs"

            options = {
              awslogs-group         = aws_cloudwatch_log_group.services[each.key].name
              awslogs-region        = var.aws_region
              awslogs-stream-prefix = each.key
            }
          }
        }
      ],

      each.value.jwt == "none" ? [] : [
        {
          name      = "jwt-key-writer"
          image     = "public.ecr.aws/docker/library/alpine:3.21"
          essential = false

          command = [
            "sh",
            "-c",
            each.value.jwt == "private" ?
            "mkdir -p /keys && printf '%s' \"$JWT_PRIVATE_KEY\" > /keys/private.pem && printf '%s' \"$JWT_PUBLIC_KEY\" > /keys/public.pem && chmod 444 /keys/private.pem /keys/public.pem" :
            "mkdir -p /keys && printf '%s' \"$JWT_PUBLIC_KEY\" > /keys/public.pem && chmod 444 /keys/public.pem"
          ]

          secrets = concat(
            [
              {
                name      = "JWT_PUBLIC_KEY"
                valueFrom = aws_secretsmanager_secret.jwt_public_key.arn
              }
            ],

            each.value.jwt == "private" ? [
              {
                name      = "JWT_PRIVATE_KEY"
                valueFrom = aws_secretsmanager_secret.jwt_private_key.arn
              }
            ] : []
          )

          mountPoints = [
            {
              sourceVolume  = "jwt-keys"
              containerPath = "/keys"
              readOnly      = false
            }
          ]

          logConfiguration = {
            logDriver = "awslogs"

            options = {
              awslogs-group         = aws_cloudwatch_log_group.services[each.key].name
              awslogs-region        = var.aws_region
              awslogs-stream-prefix = "jwt-key-writer"
            }
          }
        }
      ]
    )
  )
}

resource "aws_ecs_service" "services" {
  for_each = local.service_config

  name    = "${local.name_prefix}-${each.key}"
  cluster = aws_ecs_cluster.main.id

  task_definition = aws_ecs_task_definition.services[each.key].arn

  desired_count = 0

  launch_type      = "FARGATE"
  platform_version = "LATEST"

  network_configuration {
    subnets = aws_subnet.public[*].id

    security_groups = [
      aws_security_group.ecs.id
    ]

    assign_public_ip = true
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.service_connect.arn

    service {
      port_name      = each.value.port_name
      discovery_name = each.key

      client_alias {
        dns_name = each.key
        port     = each.value.port
      }
    }
  }

  dynamic "load_balancer" {
    for_each = each.key == "api-gateway" ? [1] : []

    content {
      target_group_arn = aws_lb_target_group.gateway.arn
      container_name   = "api-gateway"
      container_port   = 8080
    }
  }

  health_check_grace_period_seconds = each.key == "api-gateway" ? 90 : null

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  lifecycle {
    ignore_changes = [
      task_definition,
      desired_count
    ]
  }
}

resource "aws_ecs_task_definition" "database_bootstrap" {
  family = "${local.name_prefix}-database-bootstrap"

  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  cpu    = "256"
  memory = "512"

  execution_role_arn = aws_iam_role.ecs_execution.arn
  task_role_arn      = aws_iam_role.ecs_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "database-bootstrap"
      image     = "public.ecr.aws/docker/library/postgres:17-alpine"
      essential = true

      environment = [
        {
          name  = "PGHOST"
          value = aws_db_instance.postgres.address
        },
        {
          name  = "PGPORT"
          value = "5432"
        }
      ]

      secrets = [
        {
          name      = "PGUSER"
          valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:username::"
        },
        {
          name      = "PGPASSWORD"
          valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:password::"
        }
      ]

      command = [
        "sh",
        "-c",
        <<-EOT
        set -eu

        for db in orderflow_user orderflow_inventory orderflow_order orderflow_notification
        do
          if ! psql -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$${db}'" | grep -q 1
          then
            psql -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"$${db}\";"
          fi
        done
        EOT
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          awslogs-group         = aws_cloudwatch_log_group.database_bootstrap.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "database-bootstrap"
        }
      }
    }
  ])
}
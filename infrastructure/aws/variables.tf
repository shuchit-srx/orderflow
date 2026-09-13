variable "project_name" {
  type    = string
  default = "orderflow"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "vpc_cidr" {
  type    = string
  default = "10.20.0.0/16"
}

variable "postgres_engine_version" {
  type    = string
  default = "17.11"
}

variable "postgres_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "postgres_allocated_storage" {
  type    = number
  default = 20
}

variable "db_master_username" {
  type    = string
  default = "orderflow_admin"
}

variable "redis_engine_version" {
  type    = string
  default = "7.1"
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "rabbitmq_engine_version" {
  type    = string
  default = "3.13"
}

variable "rabbitmq_instance_type" {
  type    = string
  default = "mq.t3.micro"
}

variable "rabbitmq_username" {
  type    = string
  default = "orderflow"
}

variable "cloudwatch_log_retention_days" {
  type    = number
  default = 14
}
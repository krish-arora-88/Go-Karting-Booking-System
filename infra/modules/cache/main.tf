# Module: cache
# Manages: ElastiCache Redis replication group, subnet group, security group

locals {
  name_prefix = "${var.app_name}-${var.environment}"
}

# --- Security Group (ingress ONLY from ECS task SG on 6379) ---
resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-redis-sg"
  description = "Allow Redis ingress from ECS tasks only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Redis from ECS tasks"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.ecs_task_sg_id]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-redis-sg"
  }
}

# --- Cache Subnet Group ---
resource "aws_elasticache_subnet_group" "main" {
  name       = "${local.name_prefix}-redis-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${local.name_prefix}-redis-subnet-group"
  }
}

# --- Redis Replication Group ---
resource "aws_elasticache_replication_group" "main" {
  replication_group_id = "${local.name_prefix}-redis"
  description          = "Redis cache for ${local.name_prefix}"

  engine               = "redis"
  engine_version       = "7.1"
  node_type            = var.node_type
  num_cache_clusters   = var.num_cache_clusters
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false

  automatic_failover_enabled = var.num_cache_clusters > 1

  snapshot_retention_limit = 1
  snapshot_window          = "05:00-06:00"

  tags = {
    Name = "${local.name_prefix}-redis"
  }
}

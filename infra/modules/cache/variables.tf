variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "app_name" {
  description = "Application name prefix"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID from networking module"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the cache subnet group"
  type        = list(string)
}

variable "ecs_task_sg_id" {
  description = "Security group ID of ECS tasks (allowed to connect to Redis)"
  type        = string
}

variable "node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t3.micro"
}

variable "num_cache_clusters" {
  description = "Number of cache clusters (1 = single node, 2+ = replication)"
  type        = number
  default     = 1
}

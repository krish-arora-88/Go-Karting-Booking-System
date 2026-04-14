variable "region" {
  description = "AWS region"
  type        = string
  default     = "ca-west-1"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging, or prod"
  }
}

variable "app_name" {
  description = "Application name used as resource name prefix"
  type        = string
  default     = "gokarting"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "gokarting"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "app"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 20
}

variable "db_multi_az" {
  description = "Enable Multi-AZ for RDS"
  type        = bool
  default     = false
}

variable "redis_node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t3.micro"
}

variable "redis_num_cache_clusters" {
  description = "Number of cache clusters (1 = no replication)"
  type        = number
  default     = 1
}

variable "ecs_task_cpu" {
  description = "ECS task CPU units (256 = 0.25 vCPU)"
  type        = number
  default     = 256
}

variable "ecs_task_memory" {
  description = "ECS task memory in MB"
  type        = number
  default     = 512
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = 1
}

variable "backend_image_tag" {
  description = "Docker image tag for the backend service"
  type        = string
  default     = "latest"
}

variable "cors_origins" {
  description = "Comma-separated CORS allowed origins"
  type        = string
  default     = "http://localhost:3000"
}

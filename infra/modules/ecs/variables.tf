variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "app_name" {
  description = "Application name prefix"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID from networking module"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for the ALB"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for ECS tasks"
  type        = list(string)
}

variable "task_execution_role_arn" {
  description = "ARN of the ECS task execution role"
  type        = string
}

variable "task_role_arn" {
  description = "ARN of the ECS task role"
  type        = string
}

variable "task_cpu" {
  description = "ECS task CPU units"
  type        = number
  default     = 256
}

variable "task_memory" {
  description = "ECS task memory in MB"
  type        = number
  default     = 512
}

variable "desired_count" {
  description = "Desired number of running tasks"
  type        = number
  default     = 1
}

variable "backend_image_tag" {
  description = "Docker image tag for the backend"
  type        = string
  default     = "latest"
}

variable "db_host" {
  description = "RDS hostname"
  type        = string
}

variable "db_port" {
  description = "RDS port"
  type        = number
  default     = 5432
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_secret_arn" {
  description = "Secrets Manager ARN for DB password"
  type        = string
}

variable "redis_host" {
  description = "ElastiCache Redis hostname"
  type        = string
}

variable "redis_port" {
  description = "ElastiCache Redis port"
  type        = number
  default     = 6379
}

variable "cors_origins" {
  description = "Comma-separated CORS allowed origins"
  type        = string
}

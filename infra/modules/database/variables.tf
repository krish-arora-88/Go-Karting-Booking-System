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
  description = "Private subnet IDs for the DB subnet group"
  type        = list(string)
}

variable "ecs_task_sg_id" {
  description = "Security group ID of ECS tasks (allowed to connect to RDS)"
  type        = string
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
}

variable "db_multi_az" {
  description = "Enable Multi-AZ"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Enable deletion protection on RDS"
  type        = bool
  default     = false
}

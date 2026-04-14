variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "app_name" {
  description = "Application name prefix"
  type        = string
}

variable "secrets_arns" {
  description = "List of Secrets Manager ARNs the task execution role may read"
  type        = list(string)
  default     = []
}

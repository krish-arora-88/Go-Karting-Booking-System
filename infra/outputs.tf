output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.ecs.alb_dns_name
}

output "ecr_backend_url" {
  description = "ECR repository URL for the backend image"
  value       = module.ecs.ecr_backend_url
}

output "db_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = module.database.db_endpoint
  sensitive   = true
}

output "redis_endpoint" {
  description = "ElastiCache Redis endpoint"
  value       = module.cache.redis_endpoint
  sensitive   = true
}

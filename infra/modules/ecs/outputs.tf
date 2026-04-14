output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "ecr_backend_url" {
  description = "ECR repository URL for the backend image"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.app.name
}

output "ecs_task_sg_id" {
  description = "Security group ID of ECS tasks (used by database and cache modules)"
  value       = aws_security_group.ecs_tasks.id
}

output "jwt_secret_arn" {
  description = "ARN of the JWT secret in Secrets Manager"
  value       = aws_secretsmanager_secret.jwt_secret.arn
}

output "db_endpoint" {
  description = "RDS instance endpoint (host:port)"
  value       = aws_db_instance.main.endpoint
}

output "db_host" {
  description = "RDS instance hostname"
  value       = aws_db_instance.main.address
}

output "db_port" {
  description = "RDS instance port"
  value       = aws_db_instance.main.port
}

output "db_secret_arn" {
  description = "ARN of the Secrets Manager secret holding DB credentials"
  value       = aws_secretsmanager_secret.db_password.arn
}

output "db_security_group_id" {
  description = "Security group ID of the RDS instance"
  value       = aws_security_group.rds.id
}

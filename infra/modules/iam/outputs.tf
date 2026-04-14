output "task_execution_role_arn" {
  description = "ARN of the ECS task execution role (used by ECS agent)"
  value       = aws_iam_role.task_execution.arn
}

output "task_role_arn" {
  description = "ARN of the ECS task role (used by application code)"
  value       = aws_iam_role.task.arn
}

# Module: iam
# Manages: ECS task execution role (ECS agent) and task role (application code)
# These are intentionally separate — execution role has infra access, task role has app access only.

locals {
  name_prefix = "${var.app_name}-${var.environment}"
}

# --- Task Execution Role (used by ECS agent, NOT application code) ---
# Permissions: pull ECR images, write CloudWatch logs, read Secrets Manager
resource "aws_iam_role" "task_execution" {
  name = "${local.name_prefix}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "task_execution_managed" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_policy" "secrets_read" {
  name        = "${local.name_prefix}-secrets-read"
  description = "Allow ECS task execution role to read specific secrets"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = length(var.secrets_arns) > 0 ? var.secrets_arns : ["arn:aws:secretsmanager:*:*:secret:${local.name_prefix}/*"]
    }]
  })
}

resource "aws_iam_role_policy_attachment" "secrets_read" {
  role       = aws_iam_role.task_execution.name
  policy_arn = aws_iam_policy.secrets_read.arn
}

# --- Task Role (used by APPLICATION code, NOT ECS agent) ---
# Minimal by design — only add permissions the application actually needs.
# Currently: CloudWatch metrics only.
resource "aws_iam_role" "task" {
  name = "${local.name_prefix}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_policy" "task_cloudwatch" {
  name        = "${local.name_prefix}-task-cloudwatch"
  description = "Allow application to publish custom CloudWatch metrics"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["cloudwatch:PutMetricData"]
      Resource = "*"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "task_cloudwatch" {
  role       = aws_iam_role.task.name
  policy_arn = aws_iam_policy.task_cloudwatch.arn
}

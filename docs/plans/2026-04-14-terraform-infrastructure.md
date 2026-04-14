# Terraform Infrastructure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Provision the complete AWS infrastructure for the Apex Go-Karting Booking System using Terraform modules (VPC, ECS Fargate, RDS PostgreSQL, ElastiCache Redis, IAM, ECR) with a GitHub Actions plan/apply pipeline.

**Architecture:** Five child modules under `infra/modules/` wired together by a root module. Networking is the foundation; all other modules consume its VPC/subnet outputs. ECS module outputs a task security group ID that database and cache modules use to scope their ingress rules. Secrets (DB password, JWT secret) live in AWS Secrets Manager and are injected into containers at runtime via the task execution role.

**Tech Stack:** Terraform 1.7, AWS provider ~> 5.0, region `ca-west-1`, state in `gokarting-tf-state-krish` S3 bucket + `gokarting-tf-locks` DynamoDB table.

---

## Task 1: Root Scaffold — backend, provider, versions

**Files:**
- Create: `infra/backend.tf`
- Create: `infra/provider.tf`
- Create: `infra/versions.tf`
- Create: `infra/variables.tf`
- Create: `infra/outputs.tf`
- Create: `infra/main.tf`
- Create: `infra/environments/dev/backend.tfvars`
- Create: `infra/environments/dev/terraform.tfvars`
- Create: `infra/environments/prod/backend.tfvars`
- Create: `infra/environments/prod/terraform.tfvars`
- Create: `infra/.gitignore`
- Create: `infra/.tflint.hcl`

**Step 1: Create `infra/backend.tf`** (partial config — values supplied per environment via `-backend-config`)

```hcl
terraform {
  backend "s3" {}
}
```

**Step 2: Create `infra/versions.tf`**

```hcl
terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}
```

**Step 3: Create `infra/provider.tf`**

```hcl
provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = var.app_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
```

**Step 4: Create `infra/variables.tf`**

```hcl
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

# --- Database ---
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

# --- Cache ---
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

# --- ECS ---
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

# --- App ---
variable "cors_origins" {
  description = "Comma-separated CORS allowed origins"
  type        = string
  default     = "http://localhost:3000"
}
```

**Step 5: Create `infra/outputs.tf`** (placeholder — will be filled after modules are built)

```hcl
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
```

**Step 6: Create `infra/main.tf`** (empty module calls — filled in Task 7)

```hcl
# Module wiring added in Task 7 after all child modules are built
```

**Step 7: Create environment config files**

`infra/environments/dev/backend.tfvars`:
```hcl
bucket         = "gokarting-tf-state-krish"
key            = "dev/terraform.tfstate"
region         = "ca-west-1"
dynamodb_table = "gokarting-tf-locks"
encrypt        = true
```

`infra/environments/dev/terraform.tfvars`:
```hcl
environment              = "dev"
region                   = "ca-west-1"
app_name                 = "gokarting"
db_instance_class        = "db.t3.micro"
db_allocated_storage     = 20
db_multi_az              = false
redis_node_type          = "cache.t3.micro"
redis_num_cache_clusters = 1
ecs_task_cpu             = 256
ecs_task_memory          = 512
ecs_desired_count        = 1
backend_image_tag        = "latest"
cors_origins             = "http://localhost:3000"
```

`infra/environments/prod/backend.tfvars`:
```hcl
bucket         = "gokarting-tf-state-krish"
key            = "prod/terraform.tfstate"
region         = "ca-west-1"
dynamodb_table = "gokarting-tf-locks"
encrypt        = true
```

`infra/environments/prod/terraform.tfvars`:
```hcl
environment              = "prod"
region                   = "ca-west-1"
app_name                 = "gokarting"
db_instance_class        = "db.t3.small"
db_allocated_storage     = 50
db_multi_az              = true
redis_node_type          = "cache.t3.small"
redis_num_cache_clusters = 2
ecs_task_cpu             = 512
ecs_task_memory          = 1024
ecs_desired_count        = 2
cors_origins             = "https://apex-gokarting.vercel.app"
```

**Step 8: Create `infra/.gitignore`**

```
.terraform/
*.tfstate
*.tfstate.backup
*.tfplan
.terraform.lock.hcl
terraform.tfvars
override.tf
override.tf.json
*_override.tf
*_override.tf.json
```

**Note:** `.terraform.lock.hcl` should actually be committed — remove it from `.gitignore` after first `terraform init`.

**Step 9: Create `infra/.tflint.hcl`**

```hcl
config {
  format = "compact"
  module = true
}

plugin "aws" {
  enabled = true
  version = "0.32.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"
}

rule "terraform_naming_convention" {
  enabled = true
  format  = "snake_case"
}

rule "terraform_required_version" {
  enabled = true
}

rule "terraform_documented_variables" {
  enabled = true
}

rule "terraform_documented_outputs" {
  enabled = true
}
```

**Step 10: Validate root scaffold**

```bash
cd infra
terraform init -backend=false
terraform validate
```
Expected: `Success! The configuration is valid.`

**Step 11: Commit**

```bash
git add infra/
git commit -m "feat(infra): scaffold root terraform module with backend, provider, variables"
```

---

## Task 2: Networking Module

**Files:**
- Create: `infra/modules/networking/main.tf`
- Create: `infra/modules/networking/variables.tf`
- Create: `infra/modules/networking/outputs.tf`

**Step 1: Create `infra/modules/networking/variables.tf`**

```hcl
variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "app_name" {
  description = "Application name prefix"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (one per AZ)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (one per AZ)"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}
```

**Step 2: Create `infra/modules/networking/main.tf`**

```hcl
# Module: networking
# Manages: VPC, public/private subnets, IGW, NAT gateway, route tables

locals {
  name_prefix = "${var.app_name}-${var.environment}"
  az_count    = length(var.public_subnet_cidrs)
}

data "aws_availability_zones" "available" {
  state = "available"
}

# --- VPC ---
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

# --- Internet Gateway ---
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

# --- Public Subnets (ALB only) ---
resource "aws_subnet" "public" {
  count = local.az_count

  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name_prefix}-public-${count.index + 1}"
    Tier = "public"
  }
}

# --- Private Subnets (ECS, RDS, Redis) ---
resource "aws_subnet" "private" {
  count = local.az_count

  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${local.name_prefix}-private-${count.index + 1}"
    Tier = "private"
  }
}

# --- Elastic IP for NAT Gateway ---
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${local.name_prefix}-nat-eip"
  }

  depends_on = [aws_internet_gateway.main]
}

# --- NAT Gateway (single, in first public subnet) ---
resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "${local.name_prefix}-nat"
  }

  depends_on = [aws_internet_gateway.main]
}

# --- Public Route Table (routes to IGW) ---
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${local.name_prefix}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count          = local.az_count
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# --- Private Route Table (routes to NAT) ---
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name = "${local.name_prefix}-private-rt"
  }
}

resource "aws_route_table_association" "private" {
  count          = local.az_count
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
```

**Step 3: Create `infra/modules/networking/outputs.tf`**

```hcl
output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = aws_subnet.private[*].id
}
```

**Step 4: Validate**

```bash
cd infra
terraform init -backend=false
terraform validate
```

**Step 5: Commit**

```bash
git add infra/modules/networking/
git commit -m "feat(infra): add networking module — VPC, subnets, IGW, NAT, route tables"
```

---

## Task 3: IAM Module

**Files:**
- Create: `infra/modules/iam/main.tf`
- Create: `infra/modules/iam/variables.tf`
- Create: `infra/modules/iam/outputs.tf`

**Step 1: Create `infra/modules/iam/variables.tf`**

```hcl
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
```

**Step 2: Create `infra/modules/iam/main.tf`**

```hcl
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
```

**Step 3: Create `infra/modules/iam/outputs.tf`**

```hcl
output "task_execution_role_arn" {
  description = "ARN of the ECS task execution role (used by ECS agent)"
  value       = aws_iam_role.task_execution.arn
}

output "task_role_arn" {
  description = "ARN of the ECS task role (used by application code)"
  value       = aws_iam_role.task.arn
}
```

**Step 4: Validate**

```bash
cd infra && terraform validate
```

**Step 5: Commit**

```bash
git add infra/modules/iam/
git commit -m "feat(infra): add iam module — separate task execution role and task role"
```

---

## Task 4: Database Module

**Files:**
- Create: `infra/modules/database/main.tf`
- Create: `infra/modules/database/variables.tf`
- Create: `infra/modules/database/outputs.tf`

**Step 1: Create `infra/modules/database/variables.tf`**

```hcl
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
```

**Step 2: Create `infra/modules/database/main.tf`**

```hcl
# Module: database
# Manages: RDS PostgreSQL, subnet group, security group, Secrets Manager password

locals {
  name_prefix = "${var.app_name}-${var.environment}"
}

# --- DB Password in Secrets Manager ---
resource "random_password" "db" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${local.name_prefix}/db/password"
  description             = "RDS PostgreSQL master password for ${local.name_prefix}"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id = aws_secretsmanager_secret.db_password.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
    dbname   = var.db_name
  })
}

# --- Security Group (ingress ONLY from ECS task SG on 5432) ---
resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "Allow PostgreSQL ingress from ECS tasks only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from ECS tasks"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.ecs_task_sg_id]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-rds-sg"
  }
}

# --- DB Subnet Group ---
resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${local.name_prefix}-db-subnet-group"
  }
}

# --- RDS PostgreSQL Instance ---
resource "aws_db_instance" "main" {
  identifier        = "${local.name_prefix}-postgres"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = var.db_instance_class
  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = var.db_multi_az

  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  deletion_protection = var.deletion_protection
  skip_final_snapshot = !var.deletion_protection

  final_snapshot_identifier = var.deletion_protection ? "${local.name_prefix}-final-snapshot" : null

  tags = {
    Name = "${local.name_prefix}-postgres"
  }
}
```

**Step 3: Create `infra/modules/database/outputs.tf`**

```hcl
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
```

**Step 4: Validate**

```bash
cd infra && terraform validate
```

**Step 5: Commit**

```bash
git add infra/modules/database/
git commit -m "feat(infra): add database module — RDS PostgreSQL with Secrets Manager"
```

---

## Task 5: Cache Module

**Files:**
- Create: `infra/modules/cache/main.tf`
- Create: `infra/modules/cache/variables.tf`
- Create: `infra/modules/cache/outputs.tf`

**Step 1: Create `infra/modules/cache/variables.tf`**

```hcl
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
```

**Step 2: Create `infra/modules/cache/main.tf`**

```hcl
# Module: cache
# Manages: ElastiCache Redis replication group, subnet group, security group

locals {
  name_prefix = "${var.app_name}-${var.environment}"
}

# --- Security Group (ingress ONLY from ECS task SG on 6379) ---
resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-redis-sg"
  description = "Allow Redis ingress from ECS tasks only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Redis from ECS tasks"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.ecs_task_sg_id]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-redis-sg"
  }
}

# --- Cache Subnet Group ---
resource "aws_elasticache_subnet_group" "main" {
  name       = "${local.name_prefix}-redis-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${local.name_prefix}-redis-subnet-group"
  }
}

# --- Redis Replication Group ---
resource "aws_elasticache_replication_group" "main" {
  replication_group_id = "${local.name_prefix}-redis"
  description          = "Redis cache for ${local.name_prefix}"

  engine               = "redis"
  engine_version       = "7.1"
  node_type            = var.node_type
  num_cache_clusters   = var.num_cache_clusters
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false # Set to true if using auth token; requires TLS in app

  automatic_failover_enabled = var.num_cache_clusters > 1

  snapshot_retention_limit = 1
  snapshot_window          = "05:00-06:00"

  tags = {
    Name = "${local.name_prefix}-redis"
  }
}
```

**Step 3: Create `infra/modules/cache/outputs.tf`**

```hcl
output "redis_endpoint" {
  description = "Redis primary endpoint address"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "redis_port" {
  description = "Redis port"
  value       = aws_elasticache_replication_group.main.port
}

output "redis_security_group_id" {
  description = "Security group ID of the Redis cluster"
  value       = aws_security_group.redis.id
}
```

**Step 4: Validate**

```bash
cd infra && terraform validate
```

**Step 5: Commit**

```bash
git add infra/modules/cache/
git commit -m "feat(infra): add cache module — ElastiCache Redis with encryption"
```

---

## Task 6: ECS Module

**Files:**
- Create: `infra/modules/ecs/main.tf`
- Create: `infra/modules/ecs/variables.tf`
- Create: `infra/modules/ecs/outputs.tf`

**Step 1: Create `infra/modules/ecs/variables.tf`**

```hcl
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

# App config passed as environment variables to the container
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

variable "jwt_secret_arn" {
  description = "Secrets Manager ARN for JWT secret"
  type        = string
}
```

**Step 2: Create `infra/modules/ecs/main.tf`**

```hcl
# Module: ecs
# Manages: ECR repository, ECS cluster, CloudWatch log group,
#          ALB + security groups, task definition, ECS service

locals {
  name_prefix    = "${var.app_name}-${var.environment}"
  container_port = 8080
  container_name = "${var.app_name}-backend"
}

data "aws_caller_identity" "current" {}

# --- ECR Repository ---
resource "aws_ecr_repository" "backend" {
  name                 = "${local.name_prefix}-backend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "${local.name_prefix}-backend"
  }
}

resource "aws_ecr_lifecycle_policy" "backend" {
  repository = aws_ecr_repository.backend.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}

# --- CloudWatch Log Group ---
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.name_prefix}"
  retention_in_days = 7
}

# --- Security Group: ALB (public internet → 80) ---
resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb-sg"
  description = "ALB: allow HTTP from internet"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTP from internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-alb-sg"
  }
}

# --- Security Group: ECS Tasks (ALB → 8080 only) ---
resource "aws_security_group" "ecs_tasks" {
  name        = "${local.name_prefix}-ecs-tasks-sg"
  description = "ECS tasks: allow ingress from ALB on app port only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "App port from ALB"
    from_port       = local.container_port
    to_port         = local.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "Allow all outbound (ECR pull, RDS, Redis, Secrets Manager)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-ecs-tasks-sg"
  }
}

# --- Application Load Balancer ---
resource "aws_lb" "main" {
  name               = "${local.name_prefix}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = {
    Name = "${local.name_prefix}-alb"
  }
}

resource "aws_lb_target_group" "app" {
  name        = "${local.name_prefix}-tg"
  port        = local.container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  tags = {
    Name = "${local.name_prefix}-tg"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

# --- JWT Secret in Secrets Manager ---
resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${local.name_prefix}/app/jwt-secret"
  description             = "JWT signing secret for ${local.name_prefix}"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = random_password.jwt_secret.result
}

# --- ECS Cluster ---
resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }
}

# --- ECS Task Definition ---
resource "aws_ecs_task_definition" "app" {
  family                   = "${local.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = var.task_execution_role_arn
  task_role_arn            = var.task_role_arn

  container_definitions = jsonencode([{
    name      = local.container_name
    image     = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
    essential = true

    portMappings = [{
      containerPort = local.container_port
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "no-kafka" },
      { name = "DB_HOST",                value = var.db_host },
      { name = "DB_PORT",                value = tostring(var.db_port) },
      { name = "DB_NAME",                value = var.db_name },
      { name = "DB_USER",                value = var.db_username },
      { name = "REDIS_HOST",             value = var.redis_host },
      { name = "REDIS_PORT",             value = tostring(var.redis_port) },
      { name = "CORS_ORIGINS",           value = var.cors_origins }
    ]

    secrets = [
      {
        name      = "DB_PASSWORD"
        valueFrom = "${var.db_secret_arn}:password::"
      },
      {
        name      = "JWT_SECRET"
        valueFrom = var.jwt_secret_arn
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${local.container_port}/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])
}

# --- ECS Service ---
resource "aws_ecs_service" "app" {
  name            = "${local.name_prefix}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = local.container_name
    container_port   = local.container_port
  }

  depends_on = [aws_lb_listener.http]

  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }
}
```

**Step 3: Create `infra/modules/ecs/outputs.tf`**

```hcl
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
```

**Step 4: Validate**

```bash
cd infra && terraform validate
```

**Step 5: Commit**

```bash
git add infra/modules/ecs/
git commit -m "feat(infra): add ecs module — ECR, cluster, ALB, task definition, Fargate service"
```

---

## Task 7: Root Module Wiring

**Files:**
- Modify: `infra/main.tf`

**Step 1: Replace `infra/main.tf` with full module wiring**

```hcl
# Root module — wires all child modules together

module "networking" {
  source = "./modules/networking"

  environment = var.environment
  app_name    = var.app_name
}

module "iam" {
  source = "./modules/iam"

  environment  = var.environment
  app_name     = var.app_name
  secrets_arns = [
    module.database.db_secret_arn,
    module.ecs.jwt_secret_arn,
  ]

  # IAM module is independent of networking — runs in parallel
}

# ECS module must come before database/cache because they need the ECS task SG ID.
# Terraform resolves this via the dependency graph automatically.
module "ecs" {
  source = "./modules/ecs"

  environment = var.environment
  app_name    = var.app_name
  region      = var.region

  vpc_id             = module.networking.vpc_id
  public_subnet_ids  = module.networking.public_subnet_ids
  private_subnet_ids = module.networking.private_subnet_ids

  task_execution_role_arn = module.iam.task_execution_role_arn
  task_role_arn           = module.iam.task_role_arn

  task_cpu      = var.ecs_task_cpu
  task_memory   = var.ecs_task_memory
  desired_count = var.ecs_desired_count

  backend_image_tag = var.backend_image_tag

  db_host       = module.database.db_host
  db_port       = module.database.db_port
  db_name       = var.db_name
  db_username   = var.db_username
  db_secret_arn = module.database.db_secret_arn

  redis_host = module.cache.redis_endpoint
  redis_port = module.cache.redis_port

  cors_origins   = var.cors_origins
  jwt_secret_arn = module.ecs.jwt_secret_arn
}

module "database" {
  source = "./modules/database"

  environment = var.environment
  app_name    = var.app_name

  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  ecs_task_sg_id     = module.ecs.ecs_task_sg_id

  db_name              = var.db_name
  db_username          = var.db_username
  db_instance_class    = var.db_instance_class
  db_allocated_storage = var.db_allocated_storage
  db_multi_az          = var.db_multi_az
  deletion_protection  = var.environment == "prod"
}

module "cache" {
  source = "./modules/cache"

  environment = var.environment
  app_name    = var.app_name

  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  ecs_task_sg_id     = module.ecs.ecs_task_sg_id

  node_type          = var.redis_node_type
  num_cache_clusters = var.redis_num_cache_clusters
}
```

**Step 2: Validate full graph**

```bash
cd infra
terraform init -backend=false
terraform validate
```
Expected: `Success! The configuration is valid.`

**Step 3: Run tflint**

```bash
tflint --init && tflint --recursive
```

**Step 4: Commit**

```bash
git add infra/main.tf
git commit -m "feat(infra): wire root module — connect all child modules"
```

---

## Task 8: GitHub Actions Terraform Pipeline

**Files:**
- Create: `.github/workflows/terraform.yml`

**Step 1: Create `.github/workflows/terraform.yml`**

```yaml
name: Terraform

on:
  push:
    branches: [main, develop]
    paths:
      - "infra/**"
      - ".github/workflows/terraform.yml"
  pull_request:
    branches: [main]
    paths:
      - "infra/**"

env:
  TF_VERSION: "1.7.5"
  WORKING_DIR: infra

permissions:
  contents: read
  pull-requests: write   # needed to post plan as PR comment

jobs:
  # ── Job 1: runs on every push / PR ──────────────────────────────────────────
  validate:
    name: Validate + Lint + Security Scan
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: ${{ env.TF_VERSION }}

      - name: Terraform format check
        working-directory: ${{ env.WORKING_DIR }}
        run: terraform fmt -check -recursive

      - name: Terraform init (no backend)
        working-directory: ${{ env.WORKING_DIR }}
        run: terraform init -backend=false

      - name: Terraform validate
        working-directory: ${{ env.WORKING_DIR }}
        run: terraform validate

      - name: tflint
        uses: terraform-linters/setup-tflint@v4
        with:
          tflint_version: latest

      - name: tflint init
        working-directory: ${{ env.WORKING_DIR }}
        run: tflint --init

      - name: tflint run
        working-directory: ${{ env.WORKING_DIR }}
        run: tflint --recursive

      - name: checkov security scan
        uses: bridgecrewio/checkov-action@master
        with:
          directory: ${{ env.WORKING_DIR }}
          framework: terraform
          quiet: true
          soft_fail: true   # advisory on first pass; remove once baseline is clean

  # ── Job 2: plan on PRs only ─────────────────────────────────────────────────
  plan:
    name: Terraform Plan
    runs-on: ubuntu-latest
    needs: validate
    if: github.event_name == 'pull_request'

    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: ${{ env.TF_VERSION }}

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id:     ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region:            ${{ secrets.AWS_REGION }}

      - name: Terraform init
        working-directory: ${{ env.WORKING_DIR }}
        run: |
          terraform init \
            -backend-config="bucket=${{ secrets.TF_BACKEND_BUCKET }}" \
            -backend-config="key=dev/terraform.tfstate" \
            -backend-config="region=${{ secrets.AWS_REGION }}" \
            -backend-config="dynamodb_table=${{ secrets.TF_BACKEND_LOCK_TABLE }}" \
            -backend-config="encrypt=true"

      - name: Terraform plan
        id: plan
        working-directory: ${{ env.WORKING_DIR }}
        run: |
          terraform plan \
            -var-file=environments/dev/terraform.tfvars \
            -no-color \
            -out=tfplan 2>&1 | tee plan_output.txt
        continue-on-error: true

      - name: Post plan as PR comment
        uses: actions/github-script@v7
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          script: |
            const fs = require('fs');
            const plan = fs.readFileSync('${{ env.WORKING_DIR }}/plan_output.txt', 'utf8');
            const truncated = plan.length > 60000 ? plan.substring(0, 60000) + '\n\n... (truncated)' : plan;
            const status = '${{ steps.plan.outcome }}' === 'success' ? '✅' : '❌';

            const body = `## ${status} Terraform Plan

<details><summary>Show Plan</summary>

\`\`\`
${truncated}
\`\`\`

</details>

*Pushed by @${{ github.actor }}, Action: \`${{ github.event_name }}\`*`;

            // Delete previous plan comments to avoid clutter
            const comments = await github.rest.issues.listComments({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
            });
            for (const c of comments.data) {
              if (c.body.includes('Terraform Plan')) {
                await github.rest.issues.deleteComment({
                  owner: context.repo.owner,
                  repo: context.repo.repo,
                  comment_id: c.id,
                });
              }
            }

            await github.rest.issues.createComment({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
              body,
            });

      - name: Fail if plan failed
        if: steps.plan.outcome == 'failure'
        run: exit 1

  # ── Job 3: apply on merge to main ───────────────────────────────────────────
  apply:
    name: Terraform Apply
    runs-on: ubuntu-latest
    needs: validate
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    environment: production   # requires manual approval in GitHub Environments

    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: ${{ env.TF_VERSION }}

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id:     ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region:            ${{ secrets.AWS_REGION }}

      - name: Terraform init
        working-directory: ${{ env.WORKING_DIR }}
        run: |
          terraform init \
            -backend-config="bucket=${{ secrets.TF_BACKEND_BUCKET }}" \
            -backend-config="key=dev/terraform.tfstate" \
            -backend-config="region=${{ secrets.AWS_REGION }}" \
            -backend-config="dynamodb_table=${{ secrets.TF_BACKEND_LOCK_TABLE }}" \
            -backend-config="encrypt=true"

      - name: Terraform apply
        working-directory: ${{ env.WORKING_DIR }}
        run: |
          terraform apply \
            -var-file=environments/dev/terraform.tfvars \
            -auto-approve
```

**Step 2: Commit**

```bash
git add .github/workflows/terraform.yml
git commit -m "feat(ci): add terraform pipeline — validate, plan-on-PR, apply-on-merge"
```

---

## Task 9: Fix Self-Reference in Root Module + Final Validation

**Issue:** In Task 7's `infra/main.tf`, the `ecs` module passes `jwt_secret_arn = module.ecs.jwt_secret_arn` to itself — that's a self-reference. The JWT secret ARN is an output of the ECS module, not an input. Fix: `jwt_secret_arn` should be removed from the ECS module's variable list and the module call. The container definition already uses `aws_secretsmanager_secret.jwt_secret.arn` directly inside the ECS module.

**Step 1: Remove `jwt_secret_arn` variable from `infra/modules/ecs/variables.tf`**

Delete the entire `variable "jwt_secret_arn"` block.

**Step 2: Remove `jwt_secret_arn` from the container definition secrets in `infra/modules/ecs/main.tf`**

In the `aws_ecs_task_definition` resource, change the secrets block to reference the secret directly:
```hcl
secrets = [
  {
    name      = "DB_PASSWORD"
    valueFrom = "${var.db_secret_arn}:password::"
  },
  {
    name      = "JWT_SECRET"
    valueFrom = aws_secretsmanager_secret_version.jwt_secret.arn
  }
]
```

**Step 3: Remove `jwt_secret_arn` from the `iam` module's `secrets_arns` in `infra/main.tf`**

```hcl
module "iam" {
  ...
  secrets_arns = [
    module.database.db_secret_arn,
    module.ecs.jwt_secret_arn,   # ← keep this — it's an OUTPUT, used by IAM, not an INPUT to ECS
  ]
}
```

This is fine — `module.ecs.jwt_secret_arn` is an output that IAM reads to scope its policy.

**Step 4: Remove the `jwt_secret_arn` input variable from the `ecs` module call in `infra/main.tf`**

Delete the line `jwt_secret_arn = module.ecs.jwt_secret_arn` from the `module "ecs"` block.

**Step 5: Final validation**

```bash
cd infra
terraform init -backend=false
terraform validate
tflint --init && tflint --recursive
```

**Step 6: Commit**

```bash
git add infra/
git commit -m "fix(infra): remove self-reference in ecs module, final validation pass"
```

---

## Deployment Checklist (after all tasks complete)

Before running `terraform apply` for the first time:

1. Push a Docker image to ECR (or the service will fail health checks — 0 tasks healthy)
   ```bash
   aws ecr get-login-password --region ca-west-1 | docker login --username AWS --password-stdin <ecr_url>
   docker build -t <ecr_url>:latest ./backend
   docker push <ecr_url>:latest
   ```
2. Add GitHub secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `TF_BACKEND_BUCKET`, `TF_BACKEND_LOCK_TABLE`
3. Create a `production` GitHub Environment (for apply approval gate)
4. Run:
   ```bash
   cd infra
   terraform init -backend-config=environments/dev/backend.tfvars
   terraform plan -var-file=environments/dev/terraform.tfvars
   terraform apply -var-file=environments/dev/terraform.tfvars
   ```

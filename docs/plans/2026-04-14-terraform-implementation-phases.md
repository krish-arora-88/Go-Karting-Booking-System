# Terraform Infrastructure — Phased Implementation Plan
**Project**: Apex Go-Karting Booking System
**Goal**: Provision complete AWS infrastructure from zero using Terraform 1.7 modular architecture
**Audience**: DevOps / Platform Engineering / SRE interviewers
**Status**: Phase 0 complete — 3 root files exist, 67 files to create

---

## Phase 0: Discovery Results (Complete — Do Not Re-Run)

### What Already Exists
| File | Status | Notes |
|---|---|---|
| `infra/backend.tf` | ✓ exists | Empty `backend "s3" {}` — needs config populated |
| `infra/provider.tf` | ✓ exists | AWS provider, region, default tags |
| `infra/versions.tf` | ✓ exists | TF 1.7+, AWS ~5.0, random ~3.6 |
| `infra/environments/dev/` | ✓ exists | Empty directory |
| `infra/environments/prod/` | ✓ exists | Empty directory |
| State Backend (S3 + DynamoDB) | ✓ provisioned | Already created in prior session |

### Application Environment Variables (from `backend/src/main/resources/application.yml`)
The ECS task definition must inject ALL of these:
```
DB_HOST, DB_PORT=5432, DB_NAME=gokarting, DB_USER, DB_PASSWORD
REDIS_HOST, REDIS_PORT=6379, REDIS_PASSWORD
KAFKA_SERVERS          ← set to empty/localhost; use SPRING_PROFILES_ACTIVE=no-kafka
JWT_SECRET             ← store in Secrets Manager (64+ chars, no special chars)
CORS_ORIGINS           ← set to ALB DNS or Vercel frontend URL
```

### Docker Container (from `backend/Dockerfile`)
- **Port**: 8080
- **Health check path**: `/actuator/health` (HTTP 200)
- **User**: non-root `appuser`
- **JVM flags**: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`

### AWS Region
All resources deploy to: **`ca-west-1`** (Canada West)

### Allowed APIs (Terraform AWS Provider ~5.0)
Key argument correctness notes from documentation:
- `aws_nat_gateway` requires `subnet_id` + `allocation_id` (zonal mode)
- `aws_vpc_security_group_ingress_rule` (v5 style) — one resource per rule, NOT inline blocks
- ECS: `execution_role_arn` = ECS agent; `task_role_arn` = application code (NEVER merge)
- Fargate task definition requires: `network_mode = "awsvpc"`, `requires_compatibilities = ["FARGATE"]`
- ElastiCache cluster-mode-disabled parameter group: `default.redis7` (NOT `default.redis7.cluster.on`)
- `aws_lb_target_group` for Fargate: must set `target_type = "ip"`
- `aws_db_instance`: use `db_name` (not `name`) for the initial database name

---

## Phase 1: Root Module Scaffold
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. The project root is `/Users/krish/Library/Mobile Documents/com~apple~CloudDocs/Projects/Go Karting Booking System`. Read `infra/backend.tf`, `infra/provider.tf`, `infra/versions.tf` first. Then implement Phase 1 of the infrastructure plan."

### What to Implement
Create 9 files that establish the root module skeleton. Do NOT call child modules yet (that is Phase 7).

#### `infra/variables.tf`
Declare these top-level variables:
```hcl
variable "environment"        { type = string }               # "dev" | "prod"
variable "region"             { type = string; default = "ca-west-1" }
variable "project"            { type = string; default = "gokarting" }

# Networking
variable "vpc_cidr"           { type = string; default = "10.0.0.0/16" }
variable "availability_zones" { type = list(string) }        # ["ca-west-1a","ca-west-1b"]
variable "public_subnet_cidrs"  { type = list(string) }      # ["10.0.1.0/24","10.0.2.0/24"]
variable "private_subnet_cidrs" { type = list(string) }      # ["10.0.10.0/24","10.0.20.0/24"]

# ECS / App
variable "app_image"          { type = string }              # full ECR image URI
variable "app_port"           { type = number; default = 8080 }
variable "ecs_cpu"            { type = number; default = 512 }
variable "ecs_memory"         { type = number; default = 1024 }
variable "ecs_desired_count"  { type = number; default = 1 }
variable "cors_origins"       { type = string }              # Vercel frontend URL

# Database
variable "db_instance_class"  { type = string }
variable "db_allocated_storage" { type = number; default = 20 }
variable "db_multi_az"        { type = bool;   default = false }

# Cache
variable "redis_node_type"    { type = string }
variable "redis_num_clusters" { type = number; default = 1 }

# State backend (passed through to backend.tfvars — do NOT put here, but document)
```

#### `infra/backend.tf` — Update existing file
```hcl
terraform {
  backend "s3" {
    # Values supplied via -backend-config=environments/<env>/backend.tfvars
    # key, bucket, region, dynamodb_table are all set externally
    encrypt = true
  }
}
```

#### `infra/outputs.tf`
```hcl
output "alb_dns_name"    { value = module.ecs.alb_dns_name }
output "ecr_repo_url"    { value = module.ecs.ecr_repo_url }
output "db_endpoint"     { value = module.database.db_endpoint; sensitive = true }
output "redis_endpoint"  { value = module.cache.redis_endpoint; sensitive = true }
output "ecs_cluster_name" { value = module.ecs.cluster_name }
```

#### `infra/main.tf` — Stub only (module calls wired in Phase 7)
```hcl
# Module calls added in Phase 7 after all child modules are complete.
# This file intentionally starts empty to allow terraform validate in each phase.
```

#### `infra/.gitignore`
```
.terraform/
.terraform.lock.hcl
*.tfstate
*.tfstate.backup
*.tfplan
tfplan
override.tf
override.tf.json
*_override.tf
*_override.tf.json
.terraformrc
terraform.rc
crash.log
```

#### `infra/.tflint.hcl`
```hcl
plugin "aws" {
  enabled = true
  version = "0.32.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"
}

rule "terraform_naming_convention" { enabled = true }
rule "terraform_documented_variables" { enabled = true }
rule "terraform_documented_outputs" { enabled = true }
```

#### `infra/environments/dev/backend.tfvars`
```hcl
bucket         = "gokarting-tf-state-krish"
key            = "dev/terraform.tfstate"
region         = "ca-west-1"
dynamodb_table = "gokarting-tf-locks"
encrypt        = true
```

#### `infra/environments/dev/terraform.tfvars`
```hcl
environment          = "dev"
region               = "ca-west-1"
availability_zones   = ["ca-west-1a", "ca-west-1b"]
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.10.0/24", "10.0.20.0/24"]

# ECS
app_image        = "PLACEHOLDER_ECR_URI:latest"
ecs_cpu          = 512
ecs_memory       = 1024
ecs_desired_count = 1
cors_origins     = "https://your-vercel-app.vercel.app"

# Database
db_instance_class    = "db.t3.micro"
db_allocated_storage = 20
db_multi_az          = false

# Cache
redis_node_type    = "cache.t3.micro"
redis_num_clusters = 1
```

#### `infra/environments/prod/backend.tfvars`
```hcl
bucket         = "gokarting-tf-state-krish"
key            = "prod/terraform.tfstate"
region         = "ca-west-1"
dynamodb_table = "gokarting-tf-locks"
encrypt        = true
```

#### `infra/environments/prod/terraform.tfvars`
```hcl
environment          = "prod"
region               = "ca-west-1"
availability_zones   = ["ca-west-1a", "ca-west-1b"]
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.10.0/24", "10.0.20.0/24"]

# ECS
app_image         = "PLACEHOLDER_ECR_URI:latest"
ecs_cpu           = 1024
ecs_memory        = 2048
ecs_desired_count = 2
cors_origins      = "https://your-vercel-app.vercel.app"

# Database
db_instance_class    = "db.t3.small"
db_allocated_storage = 50
db_multi_az          = true

# Cache
redis_node_type    = "cache.t3.small"
redis_num_clusters = 2
```

### Verification Checklist
- [ ] `cd infra && terraform init -backend=false` — exits 0
- [ ] `terraform validate` — exits 0 (no module calls yet, just variables/outputs)
- [ ] `terraform fmt -check -recursive` — exits 0
- [ ] Grep for `password` in all `.tf` files: zero results (no hardcoded secrets)

### Anti-Pattern Guards
- Do NOT add `required_providers` to `variables.tf` — already in `versions.tf`
- Do NOT call any `module` blocks in `main.tf` yet — that creates forward references to modules that don't exist
- Do NOT put secrets in `terraform.tfvars` — passwords come from Secrets Manager

---

## Phase 2: Networking Module
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Read `infra/versions.tf` and `infra/environments/dev/terraform.tfvars` first. Implement Phase 2: the networking module."

### What to Implement
Create `infra/modules/networking/` with 3 files. This is the foundation — all other modules depend on its outputs.

#### Architecture
```
VPC 10.0.0.0/16
├── Public Subnet A (ca-west-1a)  10.0.1.0/24  → ALB lives here
├── Public Subnet B (ca-west-1b)  10.0.2.0/24  → ALB second AZ
├── Private Subnet A (ca-west-1a) 10.0.10.0/24 → ECS + RDS + Redis
└── Private Subnet B (ca-west-1b) 10.0.20.0/24 → ECS + RDS + Redis (multi-AZ)

Internet Gateway → attached to VPC
NAT Gateway (in Public Subnet A) → EIP → allows private subnets to reach ECR
Public Route Table → 0.0.0.0/0 → IGW
Private Route Table → 0.0.0.0/0 → NAT Gateway
```

#### `infra/modules/networking/variables.tf`
```hcl
variable "project"               { type = string }
variable "environment"           { type = string }
variable "vpc_cidr"              { type = string }
variable "availability_zones"    { type = list(string) }
variable "public_subnet_cidrs"   { type = list(string) }
variable "private_subnet_cidrs"  { type = list(string) }
```

#### `infra/modules/networking/main.tf`
Key resources (use v5 style — separate ingress/egress rule resources):
1. `aws_vpc.main` — `cidr_block`, `enable_dns_hostnames = true`, `enable_dns_support = true`
2. `aws_subnet.public` (count = length(var.availability_zones)) — `map_public_ip_on_launch = true`
3. `aws_subnet.private` (count = length(var.availability_zones))
4. `aws_internet_gateway.main` — `vpc_id = aws_vpc.main.id`
5. `aws_eip.nat` — `domain = "vpc"`
6. `aws_nat_gateway.main` — `subnet_id = aws_subnet.public[0].id`, `allocation_id = aws_eip.nat.id`, `depends_on = [aws_internet_gateway.main]`
7. `aws_route_table.public` — route `0.0.0.0/0` → `aws_internet_gateway.main.id`
8. `aws_route_table.private` — route `0.0.0.0/0` → `aws_nat_gateway.main.id`
9. `aws_route_table_association.public` (count)
10. `aws_route_table_association.private` (count)

#### `infra/modules/networking/outputs.tf`
```hcl
output "vpc_id"              { value = aws_vpc.main.id }
output "public_subnet_ids"   { value = aws_subnet.public[*].id }
output "private_subnet_ids"  { value = aws_subnet.private[*].id }
```

### Verification Checklist
- [ ] `terraform fmt -check modules/networking/`
- [ ] From `infra/`, add temporary `module "networking" {}` call in `main.tf`, run `terraform validate`, then remove it
- [ ] `tflint --chdir modules/networking/` — no errors
- [ ] Grep for `0.0.0.0/0` in `modules/networking/main.tf` — appears exactly twice (public route table + NAT route)

### Anti-Pattern Guards
- Do NOT assign `map_public_ip_on_launch = true` to private subnets
- Do NOT use inline `route {}` blocks inside `aws_route_table` — use `aws_route` resource or include routes inline but consistently
- Do NOT skip `depends_on = [aws_internet_gateway.main]` on NAT Gateway — causes deletion order issues
- Only ONE NAT Gateway for dev (single AZ, cost-saving). Prod uses same single NAT for simplicity (add multi-AZ NAT as a future enhancement if cost is approved)

---

## Phase 3: IAM Module
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Read `infra/modules/networking/outputs.tf` for reference on module output style. Implement Phase 3: the IAM module. This demonstrates least-privilege IAM maturity — the primary interview focus."

### What to Implement
Create `infra/modules/iam/` with 3 files. Two separate roles — this separation is the #1 interview talking point.

#### Role Separation Diagram
```
┌─────────────────────────────────┐   ┌─────────────────────────────────┐
│  Task Execution Role            │   │  Task Role                      │
│  (ECS agent — not your app)     │   │  (Application code — not ECS)   │
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Trust principal:                │   │ Trust principal:                │
│   ecs-tasks.amazonaws.com       │   │   ecs-tasks.amazonaws.com       │
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Permissions:                    │   │ Permissions:                    │
│ • AmazonECSTaskExecutionRole    │   │ • logs:CreateLogGroup           │
│   (AWS managed — ECR pull,      │   │ • logs:CreateLogStream          │
│    CloudWatch log creation)     │   │ • logs:PutLogEvents             │
│ • secretsmanager:GetSecretValue │   │                                 │
│   on specific secret ARN paths  │   │ (No ECR, No Secrets Manager)    │
└─────────────────────────────────┘   └─────────────────────────────────┘
```

#### `infra/modules/iam/variables.tf`
```hcl
variable "project"          { type = string }
variable "environment"      { type = string }
variable "secret_arns"      { type = list(string); description = "ARNs the execution role can read" }
```

#### `infra/modules/iam/main.tf`
Resources:
1. `data "aws_iam_policy_document" "ecs_assume_role"` — trust policy for `ecs-tasks.amazonaws.com`
2. `aws_iam_role.task_execution` — `assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json`
3. `aws_iam_role_policy_attachment.task_execution_managed` — attach `arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy`
4. `data "aws_iam_policy_document" "secrets_read"` — allow `secretsmanager:GetSecretValue` on `var.secret_arns`
5. `aws_iam_policy.secrets_read` — policy from document
6. `aws_iam_role_policy_attachment.task_execution_secrets` — attach `aws_iam_policy.secrets_read.arn`
7. `aws_iam_role.task` — application role, same trust policy
8. `data "aws_iam_policy_document" "task_logs"` — allow `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents` on `arn:aws:logs:*:*:*`
9. `aws_iam_policy.task_logs` — policy from document
10. `aws_iam_role_policy_attachment.task_logs` — attach to `aws_iam_role.task`

#### `infra/modules/iam/outputs.tf`
```hcl
output "task_execution_role_arn" { value = aws_iam_role.task_execution.arn }
output "task_role_arn"           { value = aws_iam_role.task.arn }
```

### Verification Checklist
- [ ] `terraform fmt -check modules/iam/`
- [ ] `tflint --chdir modules/iam/` — no errors
- [ ] Grep: `aws_iam_role_policy_attachment` appears exactly 3 times (managed policy + secrets + task logs)
- [ ] Grep: `secretsmanager:GetSecretValue` — appears in execution role only, NOT task role

### Anti-Pattern Guards
- Do NOT grant `secretsmanager:*` or `*` wildcard — scope to specific `secret_arns`
- Do NOT merge both roles into one — the separation is the entire point
- Do NOT inline permissions in `aws_iam_role.managed_policy_arns` — use `aws_iam_role_policy_attachment` instead
- Do NOT add S3, SQS, DynamoDB or any other permissions to the task role unless the app actually uses them

---

## Phase 4: Database Module
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Read `infra/modules/iam/outputs.tf` and `infra/modules/networking/outputs.tf` for reference. Implement Phase 4: the database module (RDS PostgreSQL + Secrets Manager + security group)."

### What to Implement
Create `infra/modules/database/` with 3 files.

#### Security Group Rule
```
ECS Task Security Group → port 5432 → RDS Security Group
(ECS SG ID is passed in as a variable — not created here)
```

#### `infra/modules/database/variables.tf`
```hcl
variable "project"              { type = string }
variable "environment"          { type = string }
variable "vpc_id"               { type = string }
variable "private_subnet_ids"   { type = list(string) }
variable "ecs_task_sg_id"       { type = string; description = "ECS task SG — only source allowed on 5432" }
variable "instance_class"       { type = string }
variable "allocated_storage"    { type = number }
variable "multi_az"             { type = bool }
variable "db_name"              { type = string; default = "gokarting" }
variable "db_username"          { type = string; default = "app" }
```

#### `infra/modules/database/main.tf`
Resources:
1. `resource "random_password" "db"` — length=32, special=true, override_special="!#$%&*()-_=+[]{}<>:?"
2. `aws_secretsmanager_secret.db"` — name = `"${var.project}/${var.environment}/db"`
3. `aws_secretsmanager_secret_version.db"` — `secret_string = jsonencode({ username, password, db_name })`
4. `aws_security_group.rds"` — vpc_id, empty ingress/egress (rules managed separately)
5. `aws_vpc_security_group_ingress_rule.rds_from_ecs"` — `from_port=5432`, `to_port=5432`, `ip_protocol="tcp"`, `referenced_security_group_id=var.ecs_task_sg_id`
6. `aws_vpc_security_group_egress_rule.rds_all"` — `ip_protocol="-1"`, `cidr_ipv4="0.0.0.0/0"` (allows RDS to reach AWS services)
7. `aws_db_subnet_group.main"` — `subnet_ids = var.private_subnet_ids`
8. `aws_db_instance.main"` — see critical arguments below

**Critical `aws_db_instance` arguments:**
```hcl
engine                       = "postgres"
engine_version               = "16"
instance_class               = var.instance_class
allocated_storage            = var.allocated_storage
db_name                      = var.db_name       # ← use db_name, NOT name
username                     = var.db_username
password                     = random_password.db.result
db_subnet_group_name         = aws_db_subnet_group.main.name
vpc_security_group_ids       = [aws_security_group.rds.id]
multi_az                     = var.multi_az
storage_encrypted            = true
storage_type                 = "gp3"
backup_retention_period      = var.environment == "prod" ? 7 : 1
skip_final_snapshot          = var.environment == "prod" ? false : true
deletion_protection          = var.environment == "prod" ? true : false
enabled_cloudwatch_logs_exports = ["postgresql"]
```

#### `infra/modules/database/outputs.tf`
```hcl
output "db_endpoint"   { value = aws_db_instance.main.endpoint; sensitive = true }
output "db_host"       { value = aws_db_instance.main.address;  sensitive = true }
output "db_port"       { value = aws_db_instance.main.port }
output "secret_arn"    { value = aws_secretsmanager_secret.db.arn }
```

### Verification Checklist
- [ ] `terraform fmt -check modules/database/`
- [ ] `tflint --chdir modules/database/` — no errors
- [ ] `checkov -d modules/database/ --framework terraform --quiet` — CKV_AWS_17 (DB not public), CKV_AWS_16 (encryption) should pass
- [ ] Grep for `password` in `main.tf` — only appears in `aws_db_instance.main.password` (the `random_password` reference), never hardcoded
- [ ] Grep: `db_name` (not `name`) used in `aws_db_instance`

### Anti-Pattern Guards
- Do NOT use `name` instead of `db_name` in `aws_db_instance` (wrong argument, silently ignored)
- Do NOT allow `0.0.0.0/0` as ingress source on port 5432 — scope to `ecs_task_sg_id` only
- Do NOT set `publicly_accessible = true` — RDS must stay in private subnets
- Do NOT skip `storage_encrypted = true` — checkov will flag this

---

## Phase 5: Cache Module
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Read `infra/modules/database/main.tf` for the security group pattern to follow. Implement Phase 5: the ElastiCache Redis module."

### What to Implement
Create `infra/modules/cache/` with 3 files.

#### `infra/modules/cache/variables.tf`
```hcl
variable "project"            { type = string }
variable "environment"        { type = string }
variable "vpc_id"             { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "ecs_task_sg_id"     { type = string }
variable "node_type"          { type = string }
variable "num_clusters"       { type = number }
```

#### `infra/modules/cache/main.tf`
Resources:
1. `aws_security_group.redis"` — vpc_id, empty inline rules
2. `aws_vpc_security_group_ingress_rule.redis_from_ecs"` — port 6379, source = `var.ecs_task_sg_id`
3. `aws_vpc_security_group_egress_rule.redis_all"` — ip_protocol="-1", cidr_ipv4="0.0.0.0/0"
4. `aws_elasticache_subnet_group.main"` — `subnet_ids = var.private_subnet_ids`
5. `resource "random_password" "redis"` — length=32, special=false (Redis AUTH token: no special chars)
6. `aws_secretsmanager_secret.redis"` — name = `"${var.project}/${var.environment}/redis"`
7. `aws_secretsmanager_secret_version.redis"` — `secret_string = random_password.redis.result`
8. `aws_elasticache_replication_group.main"` — see critical arguments below

**Critical `aws_elasticache_replication_group` arguments:**
```hcl
replication_group_id       = "${var.project}-${var.environment}"
description                = "Redis cache for ${var.project} ${var.environment}"
node_type                  = var.node_type
port                       = 6379
parameter_group_name       = "default.redis7"    # cluster mode DISABLED
engine_version             = "7.1"
num_cache_clusters         = var.num_clusters    # 1 for dev, 2 for prod
automatic_failover_enabled = var.num_clusters > 1 ? true : false
subnet_group_name          = aws_elasticache_subnet_group.main.name
security_group_ids         = [aws_security_group.redis.id]
at_rest_encryption_enabled = true
transit_encryption_enabled = true
auth_token                 = random_password.redis.result
```

#### `infra/modules/cache/outputs.tf`
```hcl
output "redis_endpoint"      { value = aws_elasticache_replication_group.main.primary_endpoint_address; sensitive = true }
output "redis_port"          { value = 6379 }
output "redis_secret_arn"    { value = aws_secretsmanager_secret.redis.arn }
```

### Verification Checklist
- [ ] `terraform fmt -check modules/cache/`
- [ ] `tflint --chdir modules/cache/` — no errors
- [ ] `checkov -d modules/cache/ --framework terraform --quiet` — at_rest and transit encryption checks pass
- [ ] Grep: `parameter_group_name = "default.redis7"` (not `default.redis7.cluster.on`)
- [ ] Grep: `automatic_failover_enabled` — `true` only when `num_clusters > 1`

### Anti-Pattern Guards
- Do NOT use `parameter_group_name = "default.redis7.cluster.on"` — this enables cluster mode which changes the endpoint API
- Do NOT allow `0.0.0.0/0` ingress on port 6379
- Do NOT set `auth_token` to a static string — use `random_password` resource
- Do NOT set `transit_encryption_enabled = false` if `auth_token` is set — AWS rejects this combination

---

## Phase 6: ECS Module (ECR + Cluster + ALB + Task Definition + Service)
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Read `infra/environments/dev/terraform.tfvars` for app config, and `infra/modules/iam/outputs.tf` for role ARN names. Implement Phase 6: the ECS module — ECR, cluster, ALB, Fargate task definition, and service."

### What to Implement
Create `infra/modules/ecs/` with 3 files. This is the largest module.

#### `infra/modules/ecs/variables.tf`
```hcl
variable "project"                  { type = string }
variable "environment"              { type = string }
variable "vpc_id"                   { type = string }
variable "public_subnet_ids"        { type = list(string) }
variable "private_subnet_ids"       { type = list(string) }
variable "task_execution_role_arn"  { type = string }
variable "task_role_arn"            { type = string }
variable "app_image"                { type = string }
variable "app_port"                 { type = number; default = 8080 }
variable "cpu"                      { type = number }
variable "memory"                   { type = number }
variable "desired_count"            { type = number }
variable "db_host"                  { type = string; sensitive = true }
variable "db_port"                  { type = number }
variable "db_name"                  { type = string }
variable "db_secret_arn"            { type = string }
variable "redis_host"               { type = string; sensitive = true }
variable "redis_secret_arn"         { type = string }
variable "cors_origins"             { type = string }
variable "jwt_secret_arn"           { type = string }   # ARN of JWT secret in Secrets Manager
```

#### `infra/modules/ecs/main.tf`
Resources in order:

**1. ECR Repository**
```hcl
resource "aws_ecr_repository" "app" {
  name                 = "${var.project}-${var.environment}"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }
  force_delete = var.environment != "prod"
}
```

**2. ECR Lifecycle Policy** (keep last 10 images)
```hcl
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection     = { tagStatus = "any"; countType = "imageCountMoreThan"; countNumber = 10 }
      action        = { type = "expire" }
    }]
  })
}
```

**3. CloudWatch Log Group**
```hcl
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.project}-${var.environment}"
  retention_in_days = 30
}
```

**4. JWT Secret in Secrets Manager**
```hcl
resource "random_password" "jwt" {
  length  = 64
  special = false   # URL-safe for HS512
}
resource "aws_secretsmanager_secret" "jwt" {
  name = "${var.project}/${var.environment}/jwt-secret"
}
resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_password.jwt.result
}
```

**5. ECS Cluster**
```hcl
resource "aws_ecs_cluster" "main" {
  name = "${var.project}-${var.environment}"
  setting { name = "containerInsights"; value = "enabled" }
}
```

**6. Security Groups (ALB + ECS Task)**
```hcl
resource "aws_security_group" "alb" { ... }
# Ingress: 80 from 0.0.0.0/0 (aws_vpc_security_group_ingress_rule)
# Egress: all to 0.0.0.0/0 (aws_vpc_security_group_egress_rule)

resource "aws_security_group" "ecs_tasks" { ... }
# Ingress: var.app_port from ALB SG only (referenced_security_group_id)
# Egress: all to 0.0.0.0/0 (allows ECR pull, Secrets Manager, CloudWatch via NAT)
```

**7. ALB + Target Group + Listener**
```hcl
resource "aws_lb" "main" {
  name               = "${var.project}-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.public_subnet_ids
  enable_deletion_protection = var.environment == "prod"
}

resource "aws_lb_target_group" "app" {
  name        = "${var.project}-${var.environment}"
  port        = var.app_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"    # ← REQUIRED for Fargate
  health_check {
    enabled             = true
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
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
```

**8. ECS Task Definition**
```hcl
resource "aws_ecs_task_definition" "app" {
  family                   = "${var.project}-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = tostring(var.cpu)
  memory                   = tostring(var.memory)
  execution_role_arn       = var.task_execution_role_arn
  task_role_arn            = var.task_role_arn

  container_definitions = jsonencode([{
    name  = "app"
    image = var.app_image
    portMappings = [{ containerPort = var.app_port; protocol = "tcp" }]
    environment = [
      { name = "DB_HOST",               value = var.db_host },
      { name = "DB_PORT",               value = tostring(var.db_port) },
      { name = "DB_NAME",               value = var.db_name },
      { name = "REDIS_PORT",            value = "6379" },
      { name = "KAFKA_SERVERS",         value = "localhost:9092" },
      { name = "CORS_ORIGINS",          value = var.cors_origins },
      { name = "SPRING_PROFILES_ACTIVE", value = "no-kafka" },
    ]
    secrets = [
      { name = "DB_USER",       valueFrom = "${var.db_secret_arn}:username::" },
      { name = "DB_PASSWORD",   valueFrom = "${var.db_secret_arn}:password::" },
      { name = "REDIS_HOST",    valueFrom = var.redis_host },   # endpoint
      { name = "REDIS_PASSWORD", valueFrom = var.redis_secret_arn },
      { name = "JWT_SECRET",    valueFrom = var.jwt_secret_arn },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "ecs"
      }
    }
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${var.app_port}/actuator/health || exit 1"]
      interval    = 30
      timeout     = 10
      retries     = 3
      startPeriod = 60
    }
  }])
}
```

**9. ECS Service**
```hcl
resource "aws_ecs_service" "app" {
  name            = "${var.project}-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false   # Private subnet + NAT handles outbound
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "app"
    container_port   = var.app_port
  }

  depends_on = [aws_lb_listener.http]
}
```

**Add data source:**
```hcl
data "aws_region" "current" {}
```

#### `infra/modules/ecs/outputs.tf`
```hcl
output "alb_dns_name"    { value = aws_lb.main.dns_name }
output "ecr_repo_url"    { value = aws_ecr_repository.app.repository_url }
output "cluster_name"    { value = aws_ecs_cluster.main.name }
output "ecs_task_sg_id"  { value = aws_security_group.ecs_tasks.id }
output "jwt_secret_arn"  { value = aws_secretsmanager_secret.jwt.arn }
```

### Verification Checklist
- [ ] `terraform fmt -check modules/ecs/`
- [ ] `tflint --chdir modules/ecs/` — no errors
- [ ] `checkov -d modules/ecs/ --framework terraform --quiet` — pass CKV_AWS_150 (ALB deletion protection prod), CKV_AWS_131 (ALB drops invalid headers)
- [ ] Grep: `target_type = "ip"` — must exist for Fargate to work
- [ ] Grep: `assign_public_ip = false` — ECS tasks in private subnets
- [ ] Grep: `0.0.0.0/0` in security group ingress — appears only on ALB port 80, NOT on ECS task SG

### Anti-Pattern Guards
- Do NOT set `target_type = "instance"` — this breaks Fargate (tasks use IPs, not instance IDs)
- Do NOT expose port 8080 directly from the ALB SG to `0.0.0.0/0` — traffic flows: Internet → ALB (80) → ECS Tasks (8080 from ALB only)
- Do NOT set `assign_public_ip = true` on ECS service — tasks in private subnets use NAT for ECR/AWS API calls
- Do NOT hardcode JWT_SECRET in environment variables — use Secrets Manager via `secrets` block
- Do NOT put `SPRING_PROFILES_ACTIVE=kafka` unless Kafka/MSK is provisioned

---

## Phase 7: Root Module Wiring
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Read all 5 module `outputs.tf` files and `infra/variables.tf` first. Implement Phase 7: wire all child modules together in `infra/main.tf`."

### What to Implement
Populate `infra/main.tf` with all 5 module calls, passing outputs of upstream modules as inputs to downstream modules. The dependency order is critical.

#### Dependency Order
```
networking → (provides vpc_id, subnet IDs)
    ↓
ecs → (creates ecs_task_sg_id — needed by database and cache)
    ↓
iam → (needs jwt_secret_arn, db_secret_arn, redis_secret_arn)
    ↓
database → (receives ecs_task_sg_id from ecs)
cache    → (receives ecs_task_sg_id from ecs)
```

**Important**: ECS module must be called BEFORE database and cache modules because it produces `ecs_task_sg_id` which database and cache need. But ECS also needs `db_host` and `redis_host` outputs from database/cache. This circular dependency is broken by:
- ECS module accepts `db_host` and `redis_host` as variables
- Database/cache modules produce their endpoints as outputs
- Wire them: `module.ecs` gets `db_host = module.database.db_host`

```hcl
# infra/main.tf

module "networking" {
  source               = "./modules/networking"
  project              = var.project
  environment          = var.environment
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}

# ECS module declared first — produces ecs_task_sg_id needed by database + cache
# BUT receives db/redis endpoints from database/cache (Terraform resolves this via its DAG)
module "ecs" {
  source                 = "./modules/ecs"
  project                = var.project
  environment            = var.environment
  vpc_id                 = module.networking.vpc_id
  public_subnet_ids      = module.networking.public_subnet_ids
  private_subnet_ids     = module.networking.private_subnet_ids
  task_execution_role_arn = module.iam.task_execution_role_arn
  task_role_arn          = module.iam.task_role_arn
  app_image              = var.app_image
  app_port               = var.app_port
  cpu                    = var.ecs_cpu
  memory                 = var.ecs_memory
  desired_count          = var.ecs_desired_count
  db_host                = module.database.db_host
  db_port                = module.database.db_port
  db_name                = "gokarting"
  db_secret_arn          = module.database.secret_arn
  redis_host             = module.cache.redis_endpoint
  redis_secret_arn       = module.cache.redis_secret_arn
  cors_origins           = var.cors_origins
  jwt_secret_arn         = ""    # ECS creates its own JWT secret internally
}

module "iam" {
  source      = "./modules/iam"
  project     = var.project
  environment = var.environment
  secret_arns = [
    module.database.secret_arn,
    module.cache.redis_secret_arn,
    module.ecs.jwt_secret_arn,
  ]
}

module "database" {
  source             = "./modules/database"
  project            = var.project
  environment        = var.environment
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  ecs_task_sg_id     = module.ecs.ecs_task_sg_id
  instance_class     = var.db_instance_class
  allocated_storage  = var.db_allocated_storage
  multi_az           = var.db_multi_az
}

module "cache" {
  source             = "./modules/cache"
  project            = var.project
  environment        = var.environment
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  ecs_task_sg_id     = module.ecs.ecs_task_sg_id
  node_type          = var.redis_node_type
  num_clusters       = var.redis_num_clusters
}
```

**Note on circular dependency**: Terraform's DAG handles the apparent cycle (ECS needs DB endpoint; DB needs ECS SG ID) because these are outputs/inputs, not resource dependencies. Terraform plans the full graph before applying.

**Remove `jwt_secret_arn` from ECS module inputs** — the ECS module creates its own JWT secret internally. Update `infra/modules/ecs/variables.tf` to remove `jwt_secret_arn` variable, and update the task definition to reference `aws_secretsmanager_secret.jwt.arn` directly (not a variable).

### Verification Checklist
- [ ] `terraform init -backend-config=environments/dev/backend.tfvars` — exits 0
- [ ] `terraform plan -var-file=environments/dev/terraform.tfvars` — shows ~80-90 resources to add, 0 errors
- [ ] `terraform validate` — exits 0
- [ ] `terraform fmt -check -recursive` — exits 0
- [ ] Verify plan shows: 2 subnets public, 2 private, 1 NAT GW, 1 RDS, 1 ElastiCache, 1 ECS cluster, 1 ALB
- [ ] `checkov -d . --framework terraform --quiet` — review findings, fix HIGH severity

### Anti-Pattern Guards
- Do NOT run `terraform apply` without verifying the Docker image exists in ECR first (ECS service will fail health checks and rollback)
- Do NOT commit `terraform.tfvars` with real credentials — `app_image` placeholder is fine
- Do NOT attempt to apply without the S3 state bucket and DynamoDB lock table already existing (they were provisioned in a prior session)

---

## Phase 8: GitHub Actions CI Pipeline
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Read `.github/workflows/ci.yml` for the existing CI style. Implement Phase 8: create `.github/workflows/terraform.yml` with validate/plan/apply jobs."

### What to Implement
Create `.github/workflows/terraform.yml` — the 3-job pipeline that shows standard DevOps practices.

#### File: `.github/workflows/terraform.yml`
```yaml
name: Terraform

on:
  push:
    branches: ["**"]
    paths: ["infra/**", ".github/workflows/terraform.yml"]
  pull_request:
    branches: [main]
    paths: ["infra/**", ".github/workflows/terraform.yml"]

env:
  TF_VERSION: "1.7.0"
  WORKING_DIR: "./infra"
  AWS_REGION: ${{ secrets.AWS_REGION }}

permissions:
  contents: read
  pull-requests: write   # needed to post plan as PR comment

jobs:
  # ─────────────────────────────────────────────────────────
  # Job 1: Validate — runs on EVERY push to any branch
  # ─────────────────────────────────────────────────────────
  validate:
    name: Validate
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ${{ env.WORKING_DIR }}
    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: ${{ env.TF_VERSION }}

      - name: Terraform Format Check
        run: terraform fmt -check -recursive

      - name: Terraform Init (no backend)
        run: terraform init -backend=false

      - name: Terraform Validate
        run: terraform validate

      - name: Install tflint
        run: |
          curl -s https://raw.githubusercontent.com/terraform-linters/tflint/master/install_linux.sh | bash

      - name: tflint
        run: |
          tflint --init
          tflint --recursive

      - name: checkov
        uses: bridgecrewio/checkov-action@master
        with:
          directory: infra/
          framework: terraform
          quiet: true
          soft_fail: false

  # ─────────────────────────────────────────────────────────
  # Job 2: Plan — runs on PULL REQUESTS only
  # ─────────────────────────────────────────────────────────
  plan:
    name: Plan
    needs: validate
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    defaults:
      run:
        working-directory: ${{ env.WORKING_DIR }}
    env:
      AWS_ACCESS_KEY_ID:     ${{ secrets.AWS_ACCESS_KEY_ID }}
      AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: ${{ env.TF_VERSION }}

      - name: Terraform Init
        run: |
          terraform init \
            -backend-config="bucket=${{ secrets.TF_BACKEND_BUCKET }}" \
            -backend-config="key=dev/terraform.tfstate" \
            -backend-config="region=${{ secrets.AWS_REGION }}" \
            -backend-config="dynamodb_table=${{ secrets.TF_BACKEND_LOCK_TABLE }}" \
            -backend-config="encrypt=true"

      - name: Terraform Plan
        id: plan
        run: |
          terraform plan \
            -var-file=environments/dev/terraform.tfvars \
            -out=tfplan \
            -no-color 2>&1 | tee plan_output.txt
          echo "exit_code=${PIPESTATUS[0]}" >> $GITHUB_OUTPUT
        continue-on-error: true

      - name: Post Plan as PR Comment
        uses: actions/github-script@v7
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          script: |
            const fs = require('fs');
            const planOutput = fs.readFileSync('${{ env.WORKING_DIR }}/plan_output.txt', 'utf8');
            const truncated = planOutput.length > 65000
              ? planOutput.substring(0, 65000) + '\n\n... output truncated ...'
              : planOutput;
            const body = `## Terraform Plan\n\`\`\`terraform\n${truncated}\n\`\`\``;
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body
            });

      - name: Fail if plan failed
        if: steps.plan.outputs.exit_code != '0'
        run: exit 1

  # ─────────────────────────────────────────────────────────
  # Job 3: Apply — runs on MERGE TO MAIN only
  # ─────────────────────────────────────────────────────────
  apply:
    name: Apply
    needs: validate
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    environment: production   # requires GitHub Environment approval gate
    defaults:
      run:
        working-directory: ${{ env.WORKING_DIR }}
    env:
      AWS_ACCESS_KEY_ID:     ${{ secrets.AWS_ACCESS_KEY_ID }}
      AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: ${{ env.TF_VERSION }}

      - name: Terraform Init
        run: |
          terraform init \
            -backend-config="bucket=${{ secrets.TF_BACKEND_BUCKET }}" \
            -backend-config="key=dev/terraform.tfstate" \
            -backend-config="region=${{ secrets.AWS_REGION }}" \
            -backend-config="dynamodb_table=${{ secrets.TF_BACKEND_LOCK_TABLE }}" \
            -backend-config="encrypt=true"

      - name: Terraform Apply
        run: |
          terraform apply \
            -var-file=environments/dev/terraform.tfvars \
            -auto-approve
```

### Verification Checklist
- [ ] YAML is valid: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/terraform.yml'))"`
- [ ] Grep: `github.event_name == 'pull_request'` — plan job condition exists
- [ ] Grep: `refs/heads/main` — apply job condition exists
- [ ] Grep: `environment: production` — apply job has approval gate
- [ ] Grep: `permissions: pull-requests: write` — exists at top level
- [ ] Grep: `TF_BACKEND_BUCKET` — used via secrets, not hardcoded

### Anti-Pattern Guards
- Do NOT hardcode `AWS_ACCESS_KEY_ID` values — use `${{ secrets.AWS_ACCESS_KEY_ID }}`
- Do NOT run apply on pull requests — only on push to main
- Do NOT skip the `environment: production` gate — prevents accidental applies
- Do NOT put the backend bucket name as a literal string — use `secrets.TF_BACKEND_BUCKET`

### Required GitHub Repository Setup (Do Before Testing)
1. **GitHub Secrets** (Settings → Secrets → Actions):
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_REGION` = `ca-west-1`
   - `TF_BACKEND_BUCKET` = `gokarting-tf-state-krish`
   - `TF_BACKEND_LOCK_TABLE` = `gokarting-tf-locks`

2. **GitHub Environment** (Settings → Environments):
   - Create environment named `production`
   - Add required reviewer (yourself) for manual approval before apply

---

## Phase 9: Final Verification & Deployment Checklist
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System Terraform IaC. Run the full verification checklist for Phase 9 before any `terraform apply`."

### Pre-Apply Verification
```bash
cd infra

# 1. Format check
terraform fmt -check -recursive

# 2. Init (dev backend)
terraform init -backend-config=environments/dev/backend.tfvars

# 3. Validate
terraform validate

# 4. Lint
tflint --init && tflint --recursive

# 5. Security scan
checkov -d . --framework terraform --quiet

# 6. Plan (review carefully — expected ~80-90 resources)
terraform plan -var-file=environments/dev/terraform.tfvars
```

### Required Before `terraform apply`
- [ ] Docker image built and pushed to ECR (else ECS service spins up 0 healthy tasks)
  ```bash
  # Build and push (run from project root after ECR repo exists)
  aws ecr get-login-password --region ca-west-1 | docker login --username AWS --password-stdin <ECR_URL>
  docker build -t gokarting-app ./backend
  docker tag gokarting-app:latest <ECR_URL>:latest
  docker push <ECR_URL>:latest
  ```
  But ECR repo is created BY Terraform — so first apply with `ecs_desired_count = 0` in terraform.tfvars, push image, then set `ecs_desired_count = 1` and apply again.

- [ ] Update `app_image` in `environments/dev/terraform.tfvars` to actual ECR URL

- [ ] GitHub Secrets configured (5 secrets listed in Phase 8)

- [ ] GitHub Environment `production` created with approval gate

### Post-Apply Verification
```bash
# Get ALB DNS name
terraform output alb_dns_name

# Test health endpoint
curl http://<alb_dns_name>/actuator/health
# Expected: {"status":"UP"}

# Test API
curl http://<alb_dns_name>/api/v1/time-slots?date=$(date +%Y-%m-%d)
```

### Interview Talking Points (What to Emphasize)
1. **Module isolation**: Each module is independently testable and has clear input/output contracts
2. **IAM role separation**: Task execution role vs task role — the #1 security interview question for ECS
3. **Security group least privilege**: ALB→ECS→RDS/Redis chain, never direct internet access to backend tiers
4. **Secrets Manager for all credentials**: Never in environment variables or tfvars
5. **State backend**: S3 + DynamoDB for remote state and locking — prevents concurrent apply conflicts
6. **PR plan comments**: Standard practice at every serious DevOps shop — reviewers see exact resource changes before merge
7. **Environment separation**: Dev/prod get different instance sizes and multi-AZ via tfvars, same code
8. **checkov + tflint**: Security scanning baked into CI, not manual

---

## Optional Phase 10: Helm Chart (Kubernetes)
**Context to paste at session start**: "Working on the Apex Go-Karting Booking System. Add a Helm chart at `/helm/` that deploys the same backend app to Kubernetes. This is an add-on for Kubernetes experience — do NOT modify any Terraform files."

### What to Implement
Create `helm/gokarting/` Helm chart with these templates:

| File | Purpose |
|---|---|
| `Chart.yaml` | Chart metadata (apiVersion v2, name, version) |
| `values.yaml` | Default values (image, replicas, resources, port) |
| `templates/deployment.yaml` | Deployment with liveness/readiness probes at `/actuator/health` |
| `templates/service.yaml` | ClusterIP service on port 8080 |
| `templates/ingress.yaml` | Ingress with `kubernetes.io/ingress.class: nginx` |
| `templates/configmap.yaml` | Non-secret env vars (DB_HOST, REDIS_HOST, etc.) |
| `templates/hpa.yaml` | HorizontalPodAutoscaler (min: 1, max: 5, CPU 70%) |
| `templates/serviceaccount.yaml` | ServiceAccount (for IRSA if on EKS) |

#### Key `values.yaml` fields:
```yaml
replicaCount: 1
image:
  repository: ""   # ECR URL (set at deploy time)
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8080

ingress:
  enabled: true
  className: nginx
  host: gokarting.example.com

resources:
  requests: { cpu: 250m, memory: 512Mi }
  limits:   { cpu: 500m, memory: 1Gi }

autoscaling:
  enabled: true
  minReplicas: 1
  maxReplicas: 5
  targetCPUUtilizationPercentage: 70

env:
  DB_HOST: ""
  DB_PORT: "5432"
  DB_NAME: gokarting
  CORS_ORIGINS: ""
  SPRING_PROFILES_ACTIVE: no-kafka

secretName: gokarting-secrets  # External secret (DB_PASSWORD, JWT_SECRET, REDIS_PASSWORD)
```

### Verification Checklist
- [ ] `helm lint helm/gokarting/` — passes
- [ ] `helm template helm/gokarting/ --set image.repository=test` — renders without error
- [ ] HPA references Deployment by correct name
- [ ] Liveness/readiness probes both point to `/actuator/health`

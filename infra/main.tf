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
}

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

  cors_origins = var.cors_origins
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

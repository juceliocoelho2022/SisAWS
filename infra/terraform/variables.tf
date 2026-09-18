variable "aws_region" {
  type    = string
  default = "sa-east-1"
}

variable "project_name" {
  type    = string
  default = "sisaws"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "db_name" {
  type    = string
  default = "sisaws"
}

variable "db_username" {
  type    = string
  default = "sisaws"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "backend_image_tag" {
  type    = string
  default = "latest"
}

variable "deploy_backend" {
  type    = bool
  default = false
}

variable "backend_desired_count" {
  type    = number
  default = 1
}

variable "backend_cpu" {
  type    = number
  default = 512
}

variable "backend_memory" {
  type    = number
  default = 1024
}

variable "use_nat_gateway" {
  description = "Use a NAT Gateway for private ECS networking."
  type        = bool
  default     = false
}

variable "enable_cloudfront" {
  description = "Enable CloudFront when the AWS account is verified for the service."
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "RDS automated backup retention in days."
  type        = number
  default     = 1
}

variable "alert_email" {
  description = "Optional email address for CloudWatch and AWS Budgets notifications."
  type        = string
  default     = ""
  sensitive   = true
}

variable "monthly_budget_usd" {
  description = "Monthly AWS cost budget for the SisAWS account."
  type        = number
  default     = 25
}

variable "ecs_cpu_alarm_threshold" {
  description = "ECS service CPU utilization percentage that triggers an alarm."
  type        = number
  default     = 75
}

variable "ecs_memory_alarm_threshold" {
  description = "ECS service memory utilization percentage that triggers an alarm."
  type        = number
  default     = 80
}

variable "rds_cpu_alarm_threshold" {
  description = "RDS CPU utilization percentage that triggers an alarm."
  type        = number
  default     = 80
}

variable "rds_free_storage_alarm_gib" {
  description = "RDS free storage threshold in GiB."
  type        = number
  default     = 5
}

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

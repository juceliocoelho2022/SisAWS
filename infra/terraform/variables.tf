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

variable "password_reset_from_email" {
  description = "Verified Amazon SES sender used for SisAWS password reset emails. Leave empty to disable email delivery."
  type        = string
  default     = ""
}

variable "password_reset_ttl_minutes" {
  description = "Lifetime of a password reset token in minutes."
  type        = number
  default     = 15

  validation {
    condition     = var.password_reset_ttl_minutes >= 5 && var.password_reset_ttl_minutes <= 60
    error_message = "password_reset_ttl_minutes must be between 5 and 60."
  }
}

variable "auth_login_ip_limit" {
  description = "Maximum login attempts per source IP in a 60 second window."
  type        = number
  default     = 10
}

variable "auth_login_email_limit" {
  description = "Maximum login attempts per email in a 5 minute window."
  type        = number
  default     = 5
}

variable "auth_forgot_ip_limit" {
  description = "Maximum password recovery requests per source IP in a 15 minute window."
  type        = number
  default     = 5
}

variable "auth_forgot_email_limit" {
  description = "Maximum password recovery requests per email in a 15 minute window."
  type        = number
  default     = 3
}

variable "auth_reset_ip_limit" {
  description = "Maximum password reset submissions per source IP in a 5 minute window."
  type        = number
  default     = 5
}

variable "auth_register_ip_limit" {
  description = "Maximum account registration attempts per source IP in a 15 minute window."
  type        = number
  default     = 5
}

variable "study_material_max_file_mb" {
  description = "Maximum academic material upload size in megabytes."
  type        = number
  default     = 25
}

variable "instructor_emails" {
  description = "Comma-separated emails promoted to INSTRUCTOR at login. Keep this value only in local terraform.tfvars."
  type        = string
  default     = ""
  sensitive   = true
}

variable "enable_bedrock_ai" {
  description = "Enable generative AI for academic materials using Amazon Bedrock."
  type        = bool
  default     = false
}

variable "bedrock_region" {
  description = "AWS region used by Amazon Bedrock Runtime."
  type        = string
  default     = "us-east-1"
}

variable "bedrock_model_id" {
  description = "Amazon Bedrock model ID used by the SisAWS study tutor."
  type        = string
  default     = "amazon.nova-lite-v1:0"
}

variable "enable_bedrock_embeddings" {
  description = "Enable semantic retrieval embeddings for academic materials using Amazon Bedrock."
  type        = bool
  default     = false
}

variable "bedrock_embedding_model_id" {
  description = "Amazon Bedrock embedding model ID used for semantic retrieval."
  type        = string
  default     = "amazon.titan-embed-text-v2:0"
}

variable "bedrock_embedding_dimensions" {
  description = "Embedding vector dimensions. Titan Text Embeddings V2 supports 256, 512, or 1024."
  type        = number
  default     = 256

  validation {
    condition     = contains([256, 512, 1024], var.bedrock_embedding_dimensions)
    error_message = "bedrock_embedding_dimensions must be 256, 512, or 1024."
  }
}

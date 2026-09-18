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

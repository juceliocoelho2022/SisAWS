variable "github_repository" {
  description = "GitHub repository allowed to assume the deployment role."
  type        = string
  default     = "juceliocoelho2022/SisAWS"
}

variable "github_repository_owner_id" {
  description = "Immutable GitHub owner ID used in the OIDC subject claim."
  type        = string
  default     = "104524218"
}

variable "github_repository_id" {
  description = "Immutable GitHub repository ID used in the OIDC subject claim."
  type        = string
  default     = "1375719728"
}

variable "github_branch" {
  description = "GitHub branch allowed to deploy to AWS."
  type        = string
  default     = "main"
}

variable "github_oidc_provider_arn" {
  description = "Existing GitHub OIDC provider ARN. Leave empty to create one in this AWS account."
  type        = string
  default     = ""
}

resource "aws_iam_openid_connect_provider" "github" {
  count = trimspace(var.github_oidc_provider_arn) == "" ? 1 : 0

  url = "https://token.actions.githubusercontent.com"

  client_id_list = ["sts.amazonaws.com"]
}

locals {
  github_repository_parts  = split("/", var.github_repository)
  github_repository_owner  = local.github_repository_parts[0]
  github_repository_name   = local.github_repository_parts[1]
  github_oidc_provider_arn = trimspace(var.github_oidc_provider_arn) != "" ? var.github_oidc_provider_arn : aws_iam_openid_connect_provider.github[0].arn
  github_oidc_subject      = "repo:${local.github_repository_owner}@${var.github_repository_owner_id}/${local.github_repository_name}@${var.github_repository_id}:ref:refs/heads/${var.github_branch}"
}

resource "aws_iam_role" "github_deploy" {
  name = "${local.name}-github-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = local.github_oidc_provider_arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = local.github_oidc_subject
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_deploy" {
  name = "${local.name}-github-deploy"
  role = aws_iam_role.github_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "EcrLogin"
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Sid    = "PushBackendImage"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:CompleteLayerUpload",
          "ecr:DescribeImages",
          "ecr:GetDownloadUrlForLayer",
          "ecr:InitiateLayerUpload",
          "ecr:PutImage",
          "ecr:UploadLayerPart"
        ]
        Resource = aws_ecr_repository.backend.arn
      },
      {
        Sid    = "DeployBackendService"
        Effect = "Allow"
        Action = [
          "ecs:DescribeServices",
          "ecs:UpdateService"
        ]
        Resource = aws_ecs_service.backend.id
      }
    ]
  })
}

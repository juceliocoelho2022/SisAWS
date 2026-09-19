locals {
  password_reset_email_enabled = trimspace(var.password_reset_from_email) != ""
}

resource "aws_sesv2_email_identity" "password_reset_sender" {
  count = local.password_reset_email_enabled ? 1 : 0

  email_identity = var.password_reset_from_email
}

resource "aws_iam_role_policy" "ecs_task_ses" {
  count = local.password_reset_email_enabled ? 1 : 0

  name = "${local.name}-password-reset-ses"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid      = "SendPasswordResetEmail"
      Effect   = "Allow"
      Action   = ["ses:SendEmail"]
      Resource = aws_sesv2_email_identity.password_reset_sender[0].arn
    }]
  })
}

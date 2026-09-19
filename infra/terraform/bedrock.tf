resource "aws_iam_role_policy" "ecs_task_bedrock" {
  count = var.enable_bedrock_ai ? 1 : 0

  name = "${local.name}-bedrock-study"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid      = "InvokeStudyModel"
      Effect   = "Allow"
      Action   = ["bedrock:InvokeModel"]
      Resource = "arn:aws:bedrock:${var.bedrock_region}::foundation-model/${var.bedrock_model_id}"
    }]
  })
}

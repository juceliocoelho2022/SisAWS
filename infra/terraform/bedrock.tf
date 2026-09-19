resource "aws_iam_role_policy" "ecs_task_bedrock" {
  count = var.enable_bedrock_ai || var.enable_bedrock_embeddings ? 1 : 0

  name = "${local.name}-bedrock-study"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid      = "InvokeStudyModels"
      Effect   = "Allow"
      Action   = ["bedrock:InvokeModel"]
      Resource = compact([
        var.enable_bedrock_ai ? "arn:aws:bedrock:${var.bedrock_region}::foundation-model/${var.bedrock_model_id}" : "",
        var.enable_bedrock_embeddings ? "arn:aws:bedrock:${var.bedrock_region}::foundation-model/${var.bedrock_embedding_model_id}" : ""
      ])
    }]
  })
}

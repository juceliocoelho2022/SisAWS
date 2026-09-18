# SisAWS AWS Infrastructure

Terraform foundation for the SisAWS v2 deployment.

## Architecture

- CloudFront is the public entry point.
- A private S3 bucket serves the React SPA through CloudFront OAC.
- Requests under `/api/*` are forwarded by CloudFront to the Application Load Balancer with caching disabled.
- The ALB accepts HTTP only from the AWS-managed CloudFront origin-facing prefix list.
- ECS Fargate runs the Spring Boot API in private subnets.
- RDS PostgreSQL runs in private subnets and only accepts traffic from ECS.
- Secrets Manager stores database credentials and the JWT signing value.
- CloudWatch Logs receives backend container logs.
- ECR stores backend container images.

## First deployment

1. Copy `terraform.tfvars.example` to `terraform.tfvars`.
2. Run `terraform init`, `terraform fmt -check`, `terraform validate` and `terraform plan`.
3. Apply first with `deploy_backend = false`.
4. Push the backend Docker image to the ECR repository printed by `terraform output backend_ecr_repository_url`.
5. Change `deploy_backend = true` and apply again.
6. Build the frontend with `VITE_API_URL=/api/v1 npm run build`.
7. Sync `frontend/dist` to the bucket printed by `terraform output frontend_bucket`.
8. Invalidate CloudFront after publishing a new frontend build.

## Cost note

This baseline creates resources that can generate charges, including RDS, NAT Gateway, Application Load Balancer, CloudFront and Fargate. Review AWS pricing before applying and destroy development resources when they are not needed.

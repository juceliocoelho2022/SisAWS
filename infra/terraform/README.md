# SisAWS AWS Infrastructure

Terraform foundation for the SisAWS v2 deployment.

## Profiles

### Dev — cost optimized

The default `terraform.tfvars.example` intentionally uses:

- `use_nat_gateway = false`;
- ECS Fargate tasks in public subnets with public IPs;
- backend security group accepting application traffic only from the ALB;
- RDS PostgreSQL in private subnets;
- `enable_cloudfront = false`;
- public S3 website hosting for the React SPA;
- one day of RDS automated backup retention.

This removes the NAT Gateway from the development environment and allows development to continue while an AWS account is awaiting CloudFront verification.

### Production

`terraform.prod.tfvars.example` enables:

- ECS tasks in private subnets;
- NAT Gateway for outbound traffic;
- private S3 bucket behind CloudFront OAC;
- ALB ingress restricted to the AWS-managed CloudFront origin-facing prefix list;
- seven days of RDS automated backup retention.

## First dev deployment

1. Copy the dev variables:

   `Copy-Item terraform.tfvars.example terraform.tfvars`

2. Initialize and validate:

   `terraform init`
   `terraform fmt -check`
   `terraform validate`
   `terraform plan`

3. Apply first with `deploy_backend = false`.

4. Build and push the backend Docker image to the ECR repository printed by:

   `terraform output backend_ecr_repository_url`

5. Change `deploy_backend = true` and apply again.

6. Get the API URL:

   `terraform output -raw api_url`

7. Build the frontend using that URL as `VITE_API_URL`, then sync `frontend/dist` to the S3 bucket from:

   `terraform output -raw frontend_bucket`

8. Open:

   `terraform output -raw frontend_url`

## CloudFront account verification

If AWS returns `Your account must be verified before you can add new CloudFront resources`, keep `enable_cloudfront = false` for dev and open an AWS account/billing support case for verification. After the account is released for CloudFront, use the production profile.

## Cost note

The development profile removes NAT Gateway charges, but resources such as Application Load Balancer, RDS, public IPv4, Fargate, Secrets Manager and CloudWatch can still generate charges. Destroy lab resources when they are not needed.

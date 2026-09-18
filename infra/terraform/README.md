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

## GitHub Actions deployment with OIDC

SisAWS uses GitHub OIDC federation instead of long-lived AWS access keys.

Terraform creates a deployment role restricted to:

- repository `juceliocoelho2022/SisAWS`;
- branch `main`;
- ECR push permissions only for the SisAWS backend repository;
- ECS update/describe permissions only for the SisAWS backend service.

Before the first apply, check whether the AWS account already has the GitHub OIDC provider:

```powershell
aws iam list-open-id-connect-providers `
  --query "OpenIDConnectProviderList[].Arn" `
  --output text
```

If an ARN ending in `oidc-provider/token.actions.githubusercontent.com` already exists, copy it to:

```hcl
github_oidc_provider_arn = "arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com"
```

Otherwise leave `github_oidc_provider_arn = ""` and Terraform will create it.

After applying, get the role ARN:

```powershell
terraform output -raw github_actions_deploy_role_arn
```

Then configure these GitHub Repository Variables:

- `SISAWS_AWS_DEPLOY_ROLE_ARN` = Terraform output above;
- `SISAWS_AWS_REGION` = `sa-east-1`;
- `SISAWS_AWS_DEPLOY_ENABLED` = `true`.

The deployment job runs only on pushes to `main`, after backend, frontend and Terraform CI jobs succeed. It builds the backend image, publishes both the commit SHA and `latest` tags to ECR, forces a new ECS deployment and waits for the service to become stable.


## Monitoring and cost guardrails

Terraform creates CloudWatch alarms for:

- ECS service CPU utilization;
- ECS service memory utilization;
- ALB unhealthy targets;
- RDS CPU utilization;
- RDS free storage.

The default development thresholds are intentionally conservative and can be adjusted in `terraform.tfvars`.

A monthly AWS Cost Budget is also created. The development default is USD 25. AWS Budgets is an alerting mechanism and does not stop resources or cap charges.

To receive notifications, set an email address only in your local `terraform.tfvars`:

```hcl
alert_email = "your-email@example.com"
```

When configured, Terraform creates an SNS email subscription for CloudWatch alarms and budget notifications at 80% actual, 100% actual, and 100% forecasted spend. Confirm the SNS subscription from the AWS email before relying on CloudWatch email alerts.

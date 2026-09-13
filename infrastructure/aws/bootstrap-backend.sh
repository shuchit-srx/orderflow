#!/usr/bin/env bash

set -euo pipefail

REGION="${1:-${AWS_REGION:-ap-south-1}}"
ENVIRONMENT="${2:-dev}"
PROJECT="orderflow"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
BUCKET="${PROJECT}-terraform-state-${ACCOUNT_ID}-${REGION}"

if ! aws s3api head-bucket --bucket "${BUCKET}" >/dev/null 2>&1; then
  if [ "${REGION}" = "us-east-1" ]; then
    aws s3api create-bucket \
      --bucket "${BUCKET}" \
      --region "${REGION}"
  else
    aws s3api create-bucket \
      --bucket "${BUCKET}" \
      --region "${REGION}" \
      --create-bucket-configuration "LocationConstraint=${REGION}"
  fi
fi

aws s3api put-public-access-block \
  --bucket "${BUCKET}" \
  --public-access-block-configuration \
  BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

aws s3api put-bucket-versioning \
  --bucket "${BUCKET}" \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket "${BUCKET}" \
  --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"},"BucketKeyEnabled":true}]}'

cat > backend.hcl <<EOF
bucket       = "${BUCKET}"
key          = "${PROJECT}/${ENVIRONMENT}/terraform.tfstate"
region       = "${REGION}"
encrypt      = true
use_lockfile = true
EOF

echo "${BUCKET}"
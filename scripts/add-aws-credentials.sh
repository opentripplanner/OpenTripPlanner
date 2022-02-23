#!/usr/bin/env bash
# This script will create the AWS credentials file if it does not exist.
# It is only meant to be run on CI (to create the proper
# environment for E2E tests).
mkdir -p ~/.aws

# If credentials do not exist, create file setting values to
# environment variables (which must be defined in CI).
# This should avoid any accidental overwrite on your local dev machine :)
if [ ! -f ~/.aws/credentials ]; then
cat > ~/.aws/credentials << EOL
[default]
aws_access_key_id = ${AWS_ACCESS_KEY_ID}
aws_secret_access_key = ${AWS_SECRET_ACCESS_KEY}
region = ${AWS_REGION}
EOL
fi
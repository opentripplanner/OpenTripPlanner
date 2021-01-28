#!/usr/bin/env bash
# This script will create the AWS credentials file if it does not exist. It is only meant to be run
# in a CI environment (to enable docs and builds to be uploaded to AWS S3).
mkdir -p ~/.aws

# If credentials do not exist, create file setting values to environment variables (which must be
# defined in a CI environment). This should avoid any accidental overwrite on your local dev
# machine :)
if [ ! -f ~/.aws/credentials ]; then
cat > ~/.aws/credentials << EOL
[default]
aws_access_key_id = ${AWS_ACCESS_KEY_ID}
aws_secret_access_key = ${AWS_SECRET_ACCESS_KEY}
region = ${AWS_REGION}
EOL
fi
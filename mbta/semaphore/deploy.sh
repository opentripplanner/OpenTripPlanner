#!/bin/bash
# should be run as ./semaphore/deploy.sh
# copied from the initial semaphore EB setup
set -e
export EB_VERSION=`git rev-parse --short HEAD`-`date +%s`
export GIT_DESCRIPTION=`git show -s --format=%s HEAD | cut -c -200`
export FEED_DESCRIPTION=`python mbta/semaphore/gtfs_feed_version.py`
aws s3 cp mbta_otp.zip s3://$S3_BUCKET_NAME/"$EB_APP_NAME"/"$EB_VERSION".zip
aws elasticbeanstalk create-application-version --application-name "$EB_APP_NAME" --version-label "$EB_VERSION" --source-bundle S3Bucket=$S3_BUCKET_NAME,S3Key="$EB_APP_NAME/$EB_VERSION.zip" --description "GTFS: $FEED_DESCRIPTION Git: $GIT_DESCRIPTION"
aws elasticbeanstalk update-environment --environment-name "$EB_ENV_NAME" --version-label "$EB_VERSION"
echo "Environment status: `aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME" | grep '"Status"' | cut -d: -f2  | sed -e 's/^[^"]*"//' -e 's/".*$//'`"
echo "Your environment is currently updating"; while [[ `aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME" | grep '"Status"' | cut -d: -f2  | sed -e 's/^[^"]*"//' -e 's/".*$//'` = "Updating" ]]; do sleep 2; printf "."; done
if [[ `aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME" | grep VersionLabel | cut -d: -f2 | sed -e 's/^[^"]*"//' -e 's/".*$//'` = "$EB_VERSION" ]]; then echo "The version of application code on Elastic Beanstalk matches the version that Semaphore sent in this deployment."; echo "Your environment info:"; aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME"; else echo "The version of application code on Elastic Beanstalk does not match the version that Semaphore sent in this deployment. Please check your AWS Elastic Beanstalk Console for more information."; echo "Your environment info:"; aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME"; false; fi
sleep 5; a="0"; echo "Waiting for environment health to turn Green"; while [[ `aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME" | grep '"Health":' | cut -d: -f2  | sed -e 's/^[^"]*"//' -e 's/".*$//'` != "Green" && $a -le 10 ]]; do sleep 30; a=$[$a+1]; printf "."; done; if [[ `aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME" | grep '"Health":' | cut -d: -f2 | sed -e 's/^[^"]*"//' -e 's/".*$//'` = "Green" ]]; then echo "Your environment status is Green, congrats!"; else echo "Your environment status is not Green, sorry."; false; fi;
echo "Your environment info:"; aws elasticbeanstalk describe-environments --environment-names "$EB_ENV_NAME"

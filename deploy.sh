#!/bin/sh
# Deploy Maven artifacts to the Conveyal repo on S3 
# iff this is not a pull request and the branch is master.
after_success: |
  if [ "$TRAVIS_BRANCH" = "master" ] && [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
    \# no need to run tests again
    mvn deploy -DskipTests
  fi

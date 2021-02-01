# get branch name of current branch for use in jar name
export BRANCH=$(
  if [ -n "$GITHUB_HEAD_REF" ]; then
    echo ${GITHUB_HEAD_REF#refs/heads/};
  else
    echo ${GITHUB_REF#refs/heads/};
  fi
)
echo $BRANCH
# `git describe --tags --always HEAD` returns a string of the form v0.0.0-52-ge10d02d.
# It assumes you have pushed a tag on a commit on github (e.g. a commit on the dev branch).
# If for some reason git can't find a tag, fallback with --always to a commit sha.
export JAR_VERSION=$(git describe --tags --always HEAD)
echo $JAR_VERSION
# Create a deployment folder, and a folder for the branch.
mkdir deploy
# Add the JAR file.
cp target/*-shaded.jar deploy/otp-$JAR_VERSION.jar
cp target/*-shaded.jar deploy/otp-latest-$BRANCH.jar


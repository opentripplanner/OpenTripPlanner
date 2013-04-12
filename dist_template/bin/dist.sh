#!/bin/bash

# build OTP distribution

BASE="/home/abyrd/git/OpenTripPlanner"
WEBAPPS="api-webapp geocoder webapp analyst-client"
JARS="graph-builder"
WINSTONE_URL="http://sourceforge.net/projects/winstone/files/winstone/v0.9.10/winstone-0.9.10.jar/download"

cd $BASE

mvn clean package
# check for build failure
if [ $? -ne 0 ]; then
   echo "MAVEN BUILD FAILED"
   exit 1
fi

rm -rf $BASE/dist
cp -R $BASE/dist_template $BASE/dist
mkdir -p $BASE/dist/webapps
mkdir -p $BASE/dist/common_libs
mkdir -p $BASE/dist/data

# fetch winstone 0.9.10 if it is not in the template -- this avoids checking it into the repo
if [ ! -f $BASE/dist/bin/winstone.jar ];
then
    wget $WINSTONE_URL -O $BASE/dist/bin/winstone.jar
fi

for WEBAPP in $WEBAPPS; do
    WEBAPP="opentripplanner-$WEBAPP"
    cp -R $BASE/$WEBAPP/target/$WEBAPP $BASE/dist/webapps/
    mv $BASE/dist/webapps/$WEBAPP/WEB-INF/lib/* $BASE/dist/common_libs/
done

for JAR in $JARS; do
    JAR="opentripplanner-$JAR"
    cp $BASE/$JAR/target/$JAR-*.jar $BASE/dist/common_libs/
    cp $BASE/$JAR/target/lib/* $BASE/dist/common_libs/
done

rm -f $BASE/otp.zip
zip -r $BASE/otp.zip dist/


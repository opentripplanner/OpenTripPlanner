#!/bin/bash
set -e
mvn -Dmaven.repo.local="${SEMAPHORE_CACHE_DIR}/.m2/" clean install -Dmaven.test.skip=true -Dgpg.skip -Dmaven.javadoc.skip=true
cp ./target/otp-1.4.0-SNAPSHOT-shaded.jar .
java -Xmx8G -jar otp-1.4.0-SNAPSHOT-shaded.jar --build var/graphs/mbta/ --basePath var/

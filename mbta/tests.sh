#!/bin/bash
set -e
mvn -Dmaven.repo.local="${SEMAPHORE_CACHE_DIR:-~}/.m2/" clean test -Dgpg.skip -Dmaven.javadoc.skip=true -DargLine="-Xmx4G"
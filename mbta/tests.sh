#!/bin/bash
set -e
mvn -Dmaven.repo.local="${SEMAPHORE_CACHE_DIR:-$HOME}/.m2/" clean test -Dgpg.skip -Dmaven.javadoc.skip=true

#!/bin/bash
set -e

export PIPENV_VERBOSITY=-1
export PIPENV_CACHE_DIR=$SEMAPHORE_CACHE_DIR

pushd "${SEMAPHORE_PROJECT_DIR}/mbta"
/usr/bin/env python3 -m pip install -U pip pipenv
/usr/bin/env python3 -m pipenv sync -d
/usr/bin/env python3 -m pipenv run python3 -m semaphore.integration_tests
popd
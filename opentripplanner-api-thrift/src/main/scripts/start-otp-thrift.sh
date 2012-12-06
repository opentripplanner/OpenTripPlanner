#!/bin/bash
OTP_THRIFT_HOME=$(dirname $0)/..
find ${OTP_THRIFT_HOME} -iname "opentripplanner-api-thrift*.jar" | xargs -I {} java -jar {} "$@"

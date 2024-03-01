FROM maven:3-eclipse-temurin-21
MAINTAINER Reittiopas version: 0.1

ENV OTP_ROOT="/opt/opentripplanner"

WORKDIR ${OTP_ROOT}

ADD pom.xml ${OTP_ROOT}/pom.xml
ADD src ${OTP_ROOT}/src
ADD doc-templates ${OTP_ROOT}/doc-templates
ADD docs ${OTP_ROOT}/docs
ADD test ${OTP_ROOT}/test
add .git ${OTP_ROOT}/.git

# Build OTP
RUN mvn package
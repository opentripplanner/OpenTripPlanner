FROM maven:3-jdk-8
MAINTAINER Reittiopas version: 0.1

RUN apt-get update && apt-get -y install curl

ENV OTP_ROOT="/opt/opentripplanner"

WORKDIR ${OTP_ROOT}

ADD pom.xml ${OTP_ROOT}/pom.xml
ADD src ${OTP_ROOT}/src
add .git ${OTP_ROOT}/.git

# Build OTP
RUN mvn package

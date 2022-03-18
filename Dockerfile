FROM openjdk:17-jre
MAINTAINER Reittiopas version: 0.1

VOLUME /opt/opentripplanner/graphs

ENV OTP_ROOT="/opt/opentripplanner"
ENV ROUTER_DATA_CONTAINER_URL="https://api.digitransit.fi/routing-data/v2/finland"

WORKDIR ${OTP_ROOT}

ADD run.sh ${OTP_ROOT}/run.sh
ADD target/*-shaded.jar ${OTP_ROOT}/otp-shaded.jar

ENV PORT=8080
EXPOSE ${PORT}
ENV ROUTER_NAME=finland
ENV JAVA_OPTS="-Xms8G -Xmx8G"

ENTRYPOINT exec ./run.sh
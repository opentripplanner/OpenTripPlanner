FROM openjdk:8-jre-alpine
MAINTAINER Reittiopas version: 0.1

RUN apk add --update curl bash && \
    rm -rf /var/cache/apk/*
VOLUME /opt/opentripplanner/graphs

ENV OTP_ROOT="/opt/opentripplanner"
ENV ROUTER_DATA_CONTAINER_URL="https://api.digitransit.fi/routing-data/v1"

WORKDIR ${OTP_ROOT}

ADD run.sh ${OTP_ROOT}/run.sh
ADD target/*-shaded.jar ${OTP_ROOT}

ENV PORT=8080
EXPOSE ${PORT}
ENV SECURE_PORT=8081
EXPOSE ${SECURE_PORT}
ENV ROUTER_NAME=finland
ENV JAVA_OPTS="-Xms8G -Xmx8G"

ENTRYPOINT exec ./run.sh

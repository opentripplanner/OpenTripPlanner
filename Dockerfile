FROM eclipse-temurin:21-jre
MAINTAINER Reittiopas version: 1.0

VOLUME /opt/opentripplanner/graphs

RUN apt-get update \
    && apt-get install -y curl bash fonts-dejavu fontconfig unzip tzdata \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

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
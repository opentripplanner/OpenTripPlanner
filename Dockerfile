FROM debian:stable as builder

RUN apt-get update && apt-get install -y --no-install-recommends curl git wget ca-certificates

WORKDIR /java/
RUN curl -Lo jre8.tar.gz https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u322-b06/OpenJDK8U-jre_x64_linux_hotspot_8u322b06.tar.gz
RUN tar xvf jre8.tar.gz && rm jre8.tar.gz

RUN curl -Lo jdk17.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.2_8.tar.gz
RUN tar xvf jdk17.tar.gz && rm jdk17.tar.gz
ENV JAVA_HOME_JDK_17=/java/jdk-17.0.2+8/

RUN curl -Lo jdk8.tar.gz https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u322-b06/OpenJDK8U-jdk_x64_linux_hotspot_8u322b06.tar.gz
RUN tar xvf jdk8.tar.gz && rm jdk8.tar.gz
ENV JAVA_HOME_JDK_8=/java/jdk8u322-b06/

RUN curl -Lo maven.tar.gz https://dlcdn.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
RUN tar xvf maven.tar.gz && rm maven.tar.gz
ENV PATH="/java/apache-maven-3.6.3/bin/:$PATH"

WORKDIR /build/onebusaway-gtfs-modules
RUN git clone --depth 1 https://github.com/mbta/onebusaway-gtfs-modules .
RUN JAVA_HOME=$JAVA_HOME_JDK_17 ./build.sh

WORKDIR /build/OpenTripPlanner
COPY . .

RUN ./mbta/update_gtfs.sh
RUN ./mbta/update_pbf.sh
ENV JAVA_HOME="$JAVA_HOME_JDK_8" PATH="$JAVA_HOME_JDK_8/bin:$PATH"
RUN ./mbta/build.sh

FROM debian:stable-slim as runner
RUN apt-get update && apt-get upgrade -y && apt-get install -y --no-install-recommends dumb-init

RUN useradd -MU otp
USER otp

COPY --from=builder --chown=otp:otp /build/OpenTripPlanner/otp-1.4.0*-shaded.jar /dist/OpenTripPlanner/
COPY --from=builder --chown=otp:otp /build/OpenTripPlanner/var/graphs/mbta/Graph.obj /dist/OpenTripPlanner/var/graphs/mbta/
COPY --from=builder --chown=otp:otp /build/OpenTripPlanner/var/graphs/mbta/*.json /dist/OpenTripPlanner/var/graphs/mbta/
COPY --from=builder --chown=otp:otp /java/jdk8u322-b06-jre /java/jdk8u322-b06-jre

ENV JAVA_HOME="/java/jdk8u322-b06-jre"
ENV PATH="$JAVA_HOME/bin:$PATH"

EXPOSE 5000

WORKDIR /dist/OpenTripPlanner
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-Xmx6G", "-jar", "otp-1.4.0-SNAPSHOT-shaded.jar", "--basePath", "var/", "--verbose", "--router", "mbta", "--server", "--port", "5000"]

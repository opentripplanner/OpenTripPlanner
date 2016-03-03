FROM maven:3-jdk-8
MAINTAINER Reittiopas version: 0.1

ENV OTP_ROOT="/opt/OpenTripPlanner"
ENV HOME=$OTP_ROOT

# Fetch maven dependencies
RUN mkdir -p ${OTP_ROOT}/graphs/hsl

WORKDIR ${OTP_ROOT}

ADD src ${OTP_ROOT}/src
ADD pom.xml ${OTP_ROOT}/pom.xml

# Build OTP
RUN mvn clean package \
  && chmod -R a+rwX .

ADD router-config.json ${OTP_ROOT}/graphs/hsl/router-config.json
ADD build-config.json ${OTP_ROOT}/graphs/hsl/build-config.json

RUN curl http://digitransit.fi/route-server/hsl.zip > ./graphs/hsl/hsl.zip \
  && curl https://s3.amazonaws.com/metro-extracts.mapzen.com/helsinki_finland.osm.pbf > ./graphs/hsl/helsinki_finland.osm.pbf \
  && java -Xmx8G -jar ./target/otp-0.20.0-SNAPSHOT-shaded.jar --build ./graphs/hsl/

RUN chown -R 9999:9999 ${OTP_ROOT}
USER 9999

EXPOSE 8080
LABEL io.openshift.expose-services 8080:http

CMD java -Xmx3G -Duser.timezone=Europe/Helsinki -jar ./target/otp-0.20.0-SNAPSHOT-shaded.jar --server --basePath . --graphs ./graphs --router hsl

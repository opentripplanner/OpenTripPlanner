FROM ubuntu:16.04

RUN apt update && \
  apt install software-properties-common -y && \
  add-apt-repository ppa:openjdk-r/ppa && \
  apt update && \
  apt install openjdk-11-jdk-headless openjdk-11-jre-headless -y && \
  apt install wget -y

COPY target/*-shaded.jar /opt/java/app.jar
COPY run-otp.sh /opt/run-otp.sh

COPY settings.json /opt/otp/settings.json
COPY *.osm.pbf /opt/otp/map.osm.pbf
COPY graphs /opt/otp/graphs

RUN mkdir /opt/otp/built
RUN chmod -R 777 /opt

EXPOSE 8080

WORKDIR "/opt/otp"

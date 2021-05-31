FROM ubuntu:16.04

RUN apt update && \
  apt install software-properties-common -y && \
  add-apt-repository ppa:openjdk-r/ppa && \
  apt update && \
  apt install openjdk-11-jdk-headless openjdk-11-jre-headless -y && \
  apt install wget -y

COPY target/*-shaded.jar /opt/java/app.jar
	
RUN chmod -R 755 /opt

EXPOSE 8080

CMD java -Xmx6G -jar /opt/java/app.jar --build --serve /opt/otp

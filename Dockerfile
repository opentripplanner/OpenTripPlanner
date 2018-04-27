FROM openjdk:8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

RUN mkdir -p base/graphs/dc

# Add DC.
ADD coord/otp-base/graphs/dc/router-config.json base/graphs/dc/router-config.json
ADD coord/otp-base/graphs/dc/Graph.obj base/graphs/dc/Graph.obj

# Add router.
ADD target/otp-1.3.0-SNAPSHOT-shaded.jar otp-1.3.0-SNAPSHOT-shaded.jar

# Expose the backend.
EXPOSE 8080

CMD java -Xmx4G -jar otp-1.3.0-SNAPSHOT-shaded.jar --server --basePath base --router dc

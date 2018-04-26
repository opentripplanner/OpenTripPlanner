FROM openjdk:8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app


RUN mkdir dc

RUN curl -L -o dc/district-of-columbia-latest.osm.pbf http://download.geofabrik.de/north-america/us/district-of-columbia-latest.osm.pbf

# Load the full WMATA transit network.
#RUN curl -L -o dc/dc.gtfs.zip https://transitfeeds.com/p/wmata/75/latest/download

ADD coord/dc/router-config.json dc/router-config.json

ADD target/otp-1.3.0-SNAPSHOT-shaded.jar otp-1.3.0-SNAPSHOT-shaded.jar

# Expose the backend.
EXPOSE 8080

CMD java -Xmx4G -jar otp-1.3.0-SNAPSHOT-shaded.jar --build dc --inMemory

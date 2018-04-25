FROM openjdk:8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# TODO(danieljy): Replace with build-from-source step eventually.
RUN wget https://repo1.maven.org/maven2/org/opentripplanner/otp/1.2.0/otp-1.2.0-shaded.jar

RUN mkdir dc

# TODO(danieljy): Load a much larger road network than just downtown DC.
RUN curl -L -o dc/district-of-columbia-latest.osm.pbf http://download.geofabrik.de/north-america/us/district-of-columbia-latest.osm.pbf

# Load the full WMATA transit network.
RUN curl -L -o dc/dc.gtfs.zip https://transitfeeds.com/p/wmata/75/latest/download

ADD coord/router-config.json dc/router-config.json

# Expose the backend.
EXPOSE 8080

CMD java -Xmx4G -jar otp-1.2.0-shaded.jar --build dc --inMemory

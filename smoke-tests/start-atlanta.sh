#! /bin/bash -e

cp ../target/otp-*-SNAPSHOT-shaded.jar otp.jar

GRAPH_DIR=atlanta
CURL_OPTS="-L --create-dirs -C -"

echo "Downloading GTFS"
curl ${CURL_OPTS} https://itsmarta.com/google_transit_feed/google_transit.zip -o ${GRAPH_DIR}/marta.gtfs.zip
echo "Downloading OSM"
curl ${CURL_OPTS} https://leonard.io/ibi/atlanta-2021-12-08.osm.pbf -o ${GRAPH_DIR}/atlanta-12-12-08.osm.pbf

echo "Building graph"
java -jar otp.jar --build --save $GRAPH_DIR

echo "Starting OTP"

nohup java -jar otp.jar --load --serve atlanta &

tail -f nohup.out
sleep 10

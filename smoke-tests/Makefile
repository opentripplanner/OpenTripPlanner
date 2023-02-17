CURL:=curl -L -\# --fail --create-dirs

otp.jar:
	cp ../target/otp-*-SNAPSHOT-shaded.jar otp.jar

atlanta/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/33.654066160697056,-84.759521484375,33.994611584814606,-84.30976867675781.pbf -o $@

atlanta/gtfs.zip:
	echo "Downloaded by OTP"

houston/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/29.65285233056443,-95.48492431640624,29.857914952075095,-95.22193908691406.pbf -o $@

houston/gtfs.zip:
	${CURL} https://ridemetro-api.ibi-transit.com/download/liveGTFS.zip -o $@

denver/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/39.65857056750545,-105.0567626953125,39.8578370694061,-104.86862182617188.pbf -o $@

denver/gtfs.zip:
	${CURL} https://www.rtd-denver.com/files/gtfs/google_transit.zip -o $@

septa/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/39.85441086976069,-75.29136657714844,40.0433862595006,-75.06202697753906.pbf -o $@

septa/gtfs.zip:
	echo "Downloaded by OTP"

portland/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/45.424388,-122.885443,45.684746,-122.44084.pbf -o $@

portland/gtfs.zip:
	echo "Downloaded by OTP"

seattle/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/47.481002,-122.459681,47.734124,-122.224433.pbf -o $@

seattle/gtfs.zip:
	echo "Downloaded by OTP"

build-%: otp.jar %/osm.pbf %/gtfs.zip
	java -Xmx6G -jar otp.jar --build --save $*

run-%: otp.jar
	java -Xmx6G -jar otp.jar --load --serve $*



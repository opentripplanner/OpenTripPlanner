CURL:=curl -L -\# --fail --create-dirs

otp.jar:
	cp ../target/otp-*-SNAPSHOT-shaded.jar otp.jar

atlanta/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/33.654066160697056,-84.759521484375,33.994611584814606,-84.30976867675781.pbf -o $@

atlanta/cobblinc.gtfs.zip:
	# Cobblinc's own feed expired at the end of January 2022. This URL contains the feed with extended dates.
	${CURL} https://internal-test-gtfs-testing.s3.amazonaws.com/Cobblinc-static-extended-dates.zip -o $@

atlanta/cobblinc-flex.gtfs.zip:
	${CURL} https://internal-test-gtfs-testing.s3.amazonaws.com/cobblinc-gtfs-flex.zip -o $@

atlanta/gtfs.zip: atlanta/cobblinc.gtfs.zip atlanta/cobblinc-flex.gtfs.zip
	${CURL} https://itsmarta.com/google_transit_feed/google_transit.zip -o $@

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

septa/bus.gtfs.zip:
	${CURL} https://www3.septa.org/developer/google_bus.zip -o $@

septa/gtfs.zip: septa/bus.gtfs.zip
	${CURL} https://www3.septa.org/developer/google_rail.zip  -o $@

build-%: otp.jar %/osm.pbf %/gtfs.zip
	java -Xmx6G -jar otp.jar --build --save $*

run-%: otp.jar
	java -Xmx6G -jar otp.jar --load --serve $*



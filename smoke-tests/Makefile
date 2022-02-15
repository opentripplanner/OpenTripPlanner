CURL:=curl -L -\# --fail --create-dirs

otp.jar:
	cp ../target/otp-*-SNAPSHOT-shaded.jar otp.jar

atlanta/osm.pbf:
	# this file can be re-hosted on IBI provided infrastructure
	${CURL} https://leonard.io/ibi/atlanta-2021-12-08.osm.pbf -o $@

atlanta/cobblinc.gtfs.zip:
	# Cobblinc's own feed expired at the end of January 2022. This URL contains the feed with extended dates.
	${CURL} https://internal-test-gtfs-testing.s3.amazonaws.com/Cobblinc-static-extended-dates.zip -o $@

atlanta/cobblinc-flex.gtfs.zip:
	${CURL} https://internal-test-gtfs-testing.s3.amazonaws.com/cobblinc-gtfs-flex.zip -o $@

atlanta/gtfs.zip: atlanta/cobblinc.gtfs.zip atlanta/cobblinc-flex.gtfs.zip
	${CURL} https://itsmarta.com/google_transit_feed/google_transit.zip -o $@

build-%: otp.jar %/osm.pbf %/gtfs.zip
	java -jar otp.jar --build --save $*

run-%: otp.jar
	java -jar otp.jar --load --serve $*



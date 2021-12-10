CURL:=curl -L -\# --fail --create-dirs

otp.jar:
	cp ../target/otp-*-SNAPSHOT-shaded.jar otp.jar

atlanta/osm.pbf:
	# this file can be re-hosted on IBI provided infrastructure
	${CURL} https://leonard.io/ibi/atlanta-2021-12-08.osm.pbf -o $@

atlanta/cobblinc.gtfs.zip:
	${CURL} https://www.arcgis.com/sharing/rest/content/items/1ce8e370a12c41b5854d8baa21f8451c/data -o $@

atlanta/cobblinc-flex.gtfs.zip:
	# once a continuously updated file is available we can replace the URL
	${CURL} https://leonard.io/ibi/cobblinc-flex-with-farezones.zip -o $@

atlanta/gtfs.zip: atlanta/cobblinc.gtfs.zip atlanta/cobblinc-flex.gtfs.zip
	${CURL} https://itsmarta.com/google_transit_feed/google_transit.zip -o $@

build-%: otp.jar %/osm.pbf %/gtfs.zip
	java -jar otp.jar --build --save $*

run-%: otp.jar
	java -jar otp.jar --load --serve $*



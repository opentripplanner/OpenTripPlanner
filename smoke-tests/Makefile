CURL:=curl -L -\# --fail --create-dirs
current_dir = $(shell pwd)

download: otp.jar

otp.jar:
	cp ../target/otp-*-SNAPSHOT-shaded.jar otp.jar


atlanta/osm.pbf:
	${CURL} https://leonard.io/ibi/atlanta-2021-12-08.osm.pbf -o $@

atlanta/cobblinc.gtfs.zip:
	${CURL} https://www.arcgis.com/sharing/rest/content/items/1ce8e370a12c41b5854d8baa21f8451c/data -o $@

atlanta/cobblinc-flex.gtfs.zip:
	${CURL} https://leonard.io/ibi/cobblinc-flex-with-farezones.zip -o $@

atlanta/gtfs.zip: atlanta/cobblinc.gtfs.zip atlanta/cobblinc-flex.gtfs.zip
	${CURL} https://itsmarta.com/google_transit_feed/google_transit.zip -o $@

build-%: otp.jar %/osm.pbf %/gtfs.zip
	java -jar otp.jar --build --save $*

run-%: otp.jar
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -Dlogback.configurationFile=${current_dir}/logback.xml -jar otp.jar --load --serve $*



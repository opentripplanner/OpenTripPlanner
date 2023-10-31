CURL:=curl -L -\# --fail --create-dirs

otp.jar:
	cp ../target/otp-*-SNAPSHOT-shaded.jar otp.jar

atlanta/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/33.654066160697056,-84.759521484375,33.994611584814606,-84.30976867675781.pbf -o $@

houston/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/29.65285233056443,-95.48492431640624,29.857914952075095,-95.22193908691406.pbf -o $@

denver/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/39.65857056750545,-105.0567626953125,39.8578370694061,-104.86862182617188.pbf -o $@

septa/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/39.85441086976069,-75.29136657714844,40.0433862595006,-75.06202697753906.pbf -o $@

portland/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/45.424388,-122.885443,45.684746,-122.44084.pbf -o $@

seattle/osm.pbf:
	${CURL} http://osm.ibi-transit.com/vex/47.50925,-122.47458,47.786105,-122.224433.pbf -o $@

build-%: otp.jar %/osm.pbf
	java -Xmx6G -jar otp.jar --build --save $*

run-%: otp.jar
	java -Xmx6G -jar otp.jar --load --serve $*



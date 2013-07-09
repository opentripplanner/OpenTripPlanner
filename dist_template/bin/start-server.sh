java -Xmx2048m -jar bin/winstone.jar --webappsDir=webapps --httpPort=8080 --ajp13Port=-1 --commonLibFolder=common_libs

echo "open http://localhost:8080/opentripplanner-webapp in a web browser"

# Building the graph is disabled temporarily
# java -Xmx6G -jar ../java/app.jar --build . --save && \
java -Xmx6G -jar ../java/app.jar --load --port 8080 --securePort 8081 .

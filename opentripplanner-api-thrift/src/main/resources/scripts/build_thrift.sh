thrift -r -gen java:beans opentripplanner-api-thrift/src/main/thrift/api.thrift
rm -rf opentripplanner-api-thrift/src/main/java/org/opentripplanner/api/thrift/definition/*.java
mkdir -p opentripplanner-api-thrift/src/main/java/org/opentripplanner/api/thrift/definition/ 
cp gen-javabean/org/opentripplanner/api/thrift/definition/*.java opentripplanner-api-thrift/src/main/java/org/opentripplanner/api/thrift/definition/

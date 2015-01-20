## Version 1.0-SNAPSHOT (branch master)
This is the most up-to-date branch where all new development occurs and is the basis for our upcoming 1.0 release of OTP. It does not currently support WAR deployment, only its built-in HTTP layer which is the same one used in the Glassfish application server. Running `target/otp.jar` with the `--server` option (use `--help` for a list of available options), resources will be available at the following URLs:
- The JS map-based trip planner is at [[http://localhost:8080/]]
- The OTP API base path is [[http://localhost:8080/otp/]]
- The trip planning endpoint is at [[http://localhost:8080/otp/routers/default/plan?...]]
- The GTFS index (stops, trips, routes, etc.) is at [[http://localhost:8080/otp/routers/default/index/routes/]]

For detailed paths of specific resources under these base paths, see the API docs at [[http://docs.opentripplanner.org/apidoc/master/]]

## Version 0.11 (branch 0.11.x)
This is the maintenance branch for release opentripplanner-0.11.0 from 24 March 2014. It is a multi-module Maven project and has several submodules which are described in the README.md in the root. It has already shifted toward the 1.0 API in that router IDs are systematically included in the API URLs (the router ID of "default" is still accepted) and the GTFS index is in the midst of an overhaul to give it a more coherent hierarchical structure. 

#### Standalone deployment 
Running `otp-core/target/otp.jar` (which contains all dependencies) you will find:
- API base path [[http://localhost:8080/otp/]]
- Trip planner endpoint [[http://localhost:8080/otp/routers/default/plan?...]]
- The GTFS index (agencies, stops, etc.) [[http://localhost:8080/otp/routers/default/transit/agencyIds]]

#### Servlet container deployment 
You can also deploy `otp-core/target/otp.war` (the core API server, including dependencies) and typically `otp-leaflet-client/target/otp-leaflet-client.war` (the Javascript map-based trip planning UI) to a Java application server such as Tomcat. Due to the fact that servlet containers will typically expose the services provided by a WAR file at a context path equal to the name of the WAR file, the API base path will probably be different when you use a servlet container.

- API base path is [[http://localhost:8080/opentripplanner-api-webapp/ws/]]
- The GTFS index (agencies, stops, etc.) [[http://localhost:8080/opentripplanner-api-webapp/ws/routers/default/transit/agencyIds]]

For detailed paths of specific resources under these base paths, see the API docs at [[http://docs.opentripplanner.org/apidoc/0.11.0/]]


## Version 0.10 (branch 0.10.x)
This is the maintenance branch for release opentripplanner-0.10.0, which contains very few updates since early 2013. It uses a version of the API that is fast disappearing, and exists mainly for those who wish to avoid changing interfaces in long-standing deployments (e.g. router IDs are provided as query parameters). It only allows deployment to a servlet container and does not contain its own HTTP layer. We recommend against using this branch if you are new to OTP.

- API base path [[http://localhost:8080/opentripplanner-api-webapp/ws/]]
- GTFS index base path [[http://localhost:8080/opentripplanner-api-webapp/ws/transit/]]

The API docs are at [[http://docs.opentripplanner.org/apidoc/0.10.0/]]
